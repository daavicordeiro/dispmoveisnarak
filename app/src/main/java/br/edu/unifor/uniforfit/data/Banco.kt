package br.edu.unifor.uniforfit.data

import android.util.Log
import br.edu.unifor.uniforfit.ui.chatbot.Assistente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Acesso ao Firebase (Authentication + Firestore).
 *
 * Coleções:
 *  - `ras/{ra}`                 → e-mail do aluno e se já criou conta (lido antes do login)
 *  - `alunos/{ra}`              → cadastro, saúde (anamnese, PAR-Q, medidas) e totais de treino
 *  - `alunos/{ra}/notificacoes` → avisos do aluno
 *  - `ranking/{ra}`             → nome e pontos da semana (visível para todos os alunos)
 *  - `gestores/{uid}`           → educadores físicos
 *  - `exercicios`, `treinos`, `lembretes`, `mensagens`, `config/seed`
 *
 * Leituras são aguardadas (suspend). Gravações não esperam a resposta do servidor:
 * o Firestore guarda no cache local e sincroniza quando houver conexão (RNF10.1).
 */
object Banco {

    private const val TAG = "Banco"

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val ouvintes = mutableListOf<ListenerRegistration>()

    /** Avisada quando dados da sessão mudam no servidor (ex.: o gestor alterou o treino). */
    var aoMudar: (() -> Unit)? = null

    sealed class Resultado {
        object Ok : Resultado()
        object CredencialInvalida : Resultado()
        object NaoEncontrado : Resultado()
        object JaTemConta : Resultado()
        object NaoEGestor : Resultado()
        data class Falha(val erro: Exception) : Resultado()
    }

    // =====================================================================
    // Autenticação
    // =====================================================================

    data class InfoRa(val email: String, val temConta: Boolean)

    suspend fun infoDoRa(ra: String): InfoRa? {
        val doc = db.collection("ras").document(ra).get().await()
        if (!doc.exists()) return null
        return InfoRa(doc.getString("email").orEmpty(), doc.getBoolean("temConta") == true)
    }

    /** RF02 — o aluno entra com o R.A.: o e-mail da conta vem de `ras/{ra}`. */
    suspend fun entrarAluno(ra: String, senha: String): Resultado = tentar {
        val info = infoDoRa(ra)
        if (info == null || !info.temConta) return@tentar Resultado.CredencialInvalida
        try {
            auth.signInWithEmailAndPassword(info.email, senha).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return@tentar Resultado.CredencialInvalida
        } catch (e: FirebaseAuthInvalidUserException) {
            return@tentar Resultado.CredencialInvalida
        }
        carregarAluno(ra)
        Resultado.Ok
    }

    /** RF03 — cria a conta do aluno matriculado e liga o uid ao cadastro. */
    suspend fun criarContaAluno(ra: String, emailDigitado: String, senha: String): Resultado = tentar {
        // O Firebase guarda o e-mail em minúsculas; as regras comparam com ele.
        val email = emailDigitado.lowercase()
        val info = infoDoRa(ra) ?: return@tentar Resultado.NaoEncontrado
        if (info.temConta) return@tentar Resultado.JaTemConta
        val usuario = try {
            auth.createUserWithEmailAndPassword(email, senha).await().user!!
        } catch (e: FirebaseAuthUserCollisionException) {
            return@tentar Resultado.JaTemConta
        }
        db.collection("alunos").document(ra).update(
            mapOf("uid" to usuario.uid, "email" to email, "onboardingPendente" to true),
        ).await()
        db.collection("ras").document(ra).set(mapOf("email" to email, "temConta" to true)).await()
        carregarAluno(ra)
        Resultado.Ok
    }

    /** RF04 — e-mail de redefinição de senha enviado pelo Firebase. */
    suspend fun recuperarSenha(ra: String): Pair<Resultado, String?> = try {
        val info = infoDoRa(ra)
        if (info == null || !info.temConta) {
            Resultado.NaoEncontrado to null
        } else {
            auth.sendPasswordResetEmail(info.email).await()
            Resultado.Ok to info.email
        }
    } catch (e: Exception) {
        Log.w(TAG, "recuperarSenha", e)
        Resultado.Falha(e) to null
    }

    /** RF27 — só contas com documento em `gestores` entram no painel. */
    suspend fun entrarGestor(email: String, senha: String): Resultado = tentar {
        val usuario = try {
            auth.signInWithEmailAndPassword(email, senha).await().user!!
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return@tentar Resultado.CredencialInvalida
        } catch (e: FirebaseAuthInvalidUserException) {
            return@tentar Resultado.CredencialInvalida
        }
        val doc = db.collection("gestores").document(usuario.uid).get().await()
        if (!doc.exists()) {
            auth.signOut()
            return@tentar Resultado.NaoEGestor
        }
        carregarGestor(usuario.uid)
        Resultado.Ok
    }

    /** RF21.5 / RF40.5 */
    fun sair() {
        pararOuvintes()
        auth.signOut()
        Dados.limpar()
        Assistente.reiniciar()
    }

    private suspend fun tentar(bloco: suspend () -> Resultado): Resultado = try {
        bloco()
    } catch (e: Exception) {
        Log.w(TAG, "falha no Firebase", e)
        Resultado.Falha(e)
    }

    // =====================================================================
    // Carga da sessão
    // =====================================================================

    private suspend fun carregarCatalogo() = coroutineScope {
        val ex = async { db.collection("exercicios").get().await() }
        val tr = async { db.collection("treinos").get().await() }
        aplicarExercicios(ex.await().documents)
        aplicarTreinos(tr.await().documents)
    }

    private fun aplicarExercicios(docs: List<DocumentSnapshot>) {
        Dados.exercicios.clear()
        Dados.exercicios += docs.mapNotNull { paraExercicio(it) }.sortedBy { it.id }
    }

    private fun aplicarTreinos(docs: List<DocumentSnapshot>) {
        Dados.treinos.clear()
        Dados.treinos += docs.mapNotNull { paraTreino(it) }.sortedBy { it.id }
        if (Dados.papel == Dados.Papel.ALUNO || Dados.aluno.ra.isNotEmpty()) aplicarCargasDoAluno()
        contarAlunosPorTreino()
    }

    /** A carga personalizada pelo aluno (RF13) substitui a prescrição nas telas dele. */
    private fun aplicarCargasDoAluno() {
        Dados.treinos.forEach { t ->
            t.itens.forEach { item -> Dados.aluno.cargas[item.exercicio.id]?.let { item.carga = it } }
        }
    }

    private fun contarAlunosPorTreino() {
        if (Dados.papel != Dados.Papel.GESTOR) return
        Dados.treinos.forEach { t -> t.alunos = Dados.alunos.count { it.treinoId == t.id } }
    }

    suspend fun carregarAluno(ra: String) = coroutineScope {
        val doc = async { db.collection("alunos").document(ra).get().await() }
        val ranking = async { db.collection("ranking").get().await() }
        val notifs = async {
            db.collection("alunos").document(ra).collection("notificacoes")
                .orderBy("criadaEm", Query.Direction.DESCENDING).get().await()
        }
        val lembretes = async { db.collection("lembretes").get().await() }
        Dados.aluno = paraAluno(doc.await()) ?: error("Aluno $ra não encontrado")
        Dados.papel = Dados.Papel.ALUNO
        carregarCatalogo()
        aplicarRanking(ranking.await().documents)
        aplicarNotificacoes(notifs.await().documents)
        Dados.professorDoAluno = Dados.aluno.professorUid.takeIf { it.isNotEmpty() }?.let { uid ->
            runCatching { db.collection("gestores").document(uid).get().await().getString("nome") }.getOrNull()
        } ?: "seu professor"
        verificarLembretes(lembretes.await().documents.map { paraLembrete(it) })
        ouvirAluno(ra)
    }

    suspend fun carregarGestor(uid: String) = coroutineScope {
        val gestor = async { db.collection("gestores").document(uid).get().await() }
        val alunos = async { db.collection("alunos").get().await() }
        val lembretes = async { db.collection("lembretes").get().await() }
        val g = gestor.await()
        Dados.papel = Dados.Papel.GESTOR
        Dados.gestorUid = uid
        Dados.nomeGestor = g.getString("nome").orEmpty()
        Dados.emailGestor = g.getString("email").orEmpty()
        Dados.avisosPainel.clear()
        (g.get("avisos") as? Map<*, *>)?.forEach { (k, v) -> Dados.avisosPainel[k.toString()] = v == true }
        aplicarAlunos(alunos.await().documents)
        aplicarLembretes(lembretes.await().documents)
        carregarCatalogo()
        ouvirGestor()
    }

    private fun aplicarRanking(docs: List<DocumentSnapshot>) {
        val semana = Dados.semanaAtual()
        Dados.ranking.clear()
        Dados.ranking += docs.map {
            val pontos = if (it.getString("semanaRef") == semana) it.getLong("pontosSemana")?.toInt() ?: 0 else 0
            PosicaoRanking(it.id, it.getString("nome").orEmpty(), pontos)
        }
    }

    private fun aplicarNotificacoes(docs: List<DocumentSnapshot>) {
        Dados.notificacoes.clear()
        Dados.notificacoes += docs.map {
            Notificacao(
                it.id, it.getString("titulo").orEmpty(), it.getString("mensagem").orEmpty(),
                it.getLong("criadaEm") ?: 0L, it.getBoolean("lida") == true,
            )
        }
    }

    private fun aplicarAlunos(docs: List<DocumentSnapshot>) {
        Dados.alunos.clear()
        Dados.alunos += docs.mapNotNull { paraAluno(it) }.sortedBy { it.nome }
        contarAlunosPorTreino()
    }

    private fun aplicarLembretes(docs: List<DocumentSnapshot>) {
        Dados.lembretes.clear()
        Dados.lembretes += docs.map { paraLembrete(it) }.sortedBy { it.id }
    }

    // ---------- Atualização em tempo real ----------

    private fun ouvirAluno(ra: String) {
        pararOuvintes()
        val alunoRef = db.collection("alunos").document(ra)
        ouvintes += alunoRef.addSnapshotListener { doc, _ ->
            if (doc == null || doc.metadata.hasPendingWrites()) return@addSnapshotListener
            paraAluno(doc)?.let {
                Dados.aluno = it
                aplicarCargasDoAluno()
                aoMudar?.invoke()
            }
        }
        ouvintes += alunoRef.collection("notificacoes").orderBy("criadaEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let { aplicarNotificacoes(it.documents); aoMudar?.invoke() }
            }
        ouvirCatalogo()
        ouvintes += db.collection("ranking").addSnapshotListener { snap, _ ->
            snap?.let { aplicarRanking(it.documents); aoMudar?.invoke() }
        }
    }

    private fun ouvirGestor() {
        pararOuvintes()
        ouvintes += db.collection("alunos").addSnapshotListener { snap, _ ->
            snap?.let { aplicarAlunos(it.documents); aoMudar?.invoke() }
        }
        ouvirCatalogo()
    }

    private fun ouvirCatalogo() {
        ouvintes += db.collection("exercicios").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            aplicarExercicios(snap.documents)
        }
        ouvintes += db.collection("treinos").addSnapshotListener { snap, _ ->
            if (snap == null || snap.metadata.hasPendingWrites() || Dados.exercicios.isEmpty()) return@addSnapshotListener
            aplicarTreinos(snap.documents)
            aoMudar?.invoke()
        }
    }

    private fun pararOuvintes() {
        ouvintes.forEach { it.remove() }
        ouvintes.clear()
    }

    // =====================================================================
    // Gravações do aluno
    // =====================================================================

    private fun gravar(tarefa: com.google.android.gms.tasks.Task<*>, oQue: String) {
        tarefa.addOnFailureListener { Log.w(TAG, "falha ao gravar $oQue", it) }
    }

    private val alunoRef get() = db.collection("alunos").document(Dados.aluno.ra)

    /** RF05 a RF07 — anamnese, PAR-Q e medidas. */
    fun salvarSaude(finalizarOnboarding: Boolean = false, novaMedicao: Boolean = false) {
        val a = Dados.aluno
        if (novaMedicao) {
            a.medicoes.add(0, Medicao(Dados.dataBr(), a.cintura, a.quadril, a.braco, a.coxa, a.gordura))
        }
        if (a.historicoPeso.isEmpty() && a.pesoKg > 0) a.historicoPeso += a.pesoKg.toFloat()
        val dados = saudeParaMapa(a).toMutableMap()
        if (finalizarOnboarding) {
            a.onboardingPendente = false
            dados["onboardingPendente"] = false
        }
        gravar(alunoRef.set(dados, SetOptions.merge()), "saúde")
    }

    /** RF18 — novo peso registrado pela calculadora de IMC. */
    fun registrarPeso(peso: Double, altura: Int) {
        val a = Dados.aluno
        a.pesoKg = peso
        a.alturaCm = altura
        a.historicoPeso += peso.toFloat()
        gravar(alunoRef.update(mapOf(
            "pesoKg" to peso, "alturaCm" to altura, "historicoPeso" to a.historicoPeso.map { it.toDouble() },
        )), "peso")
    }

    /** RF13.3 — carga personalizada; [recorde] soma a evolução e gera notificação. */
    fun salvarCarga(item: ItemTreino, novaCarga: Int, recorde: Boolean) {
        val a = Dados.aluno
        val id = item.exercicio.id
        a.ultimaCarga[id] = item.carga
        if (recorde) a.cargaEvoluidaKg += (novaCarga - item.carga).coerceAtLeast(0)
        a.cargas[id] = novaCarga
        item.carga = novaCarga
        gravar(alunoRef.update(mapOf(
            "cargas" to a.cargas.mapKeys { it.key.toString() },
            "ultimaCarga" to a.ultimaCarga.mapKeys { it.key.toString() },
            "cargaEvoluidaKg" to a.cargaEvoluidaKg,
        )), "carga")
        if (recorde) {
            notificar(a.ra, "Novo recorde 🎉", "Você bateu recorde no ${item.exercicio.nome}: ${novaCarga}kg.")
        }
    }

    /** RF15.2 / RF19.2 — grava o treino concluído, soma os pontos e atualiza o ranking. */
    fun registrarTreinoConcluido(treino: Treino) {
        val a = Dados.aluno
        val mes = Dados.mesAtual()
        val semana = Dados.semanaAtual()
        if (a.mesRef != mes) {
            a.mesRef = mes
            a.treinosMes = 0
        }
        if (a.semanaRef != semana) {
            a.posicaoSemanaPassada = Dados.posicaoRanking.coerceAtLeast(1)
            a.semanaRef = semana
            a.pontosSemana = 0
        }
        a.treinosMes++
        a.treinosTotais++
        a.pontos += Dados.PONTOS_POR_TREINO
        a.pontosSemana += Dados.PONTOS_POR_TREINO
        if (Dados.hoje() !in a.concluidos) a.concluidos += Dados.hoje()
        treino.itens.forEach { item ->
            Dados.chaveHistorico(item.exercicio)?.let { chave -> a.historicoCarga[chave]?.add(item.carga.toFloat()) }
        }
        gravar(alunoRef.update(estatisticasParaMapa(a)), "treino concluído")
        gravar(db.collection("ranking").document(a.ra).set(mapOf(
            "nome" to a.nome, "pontosSemana" to a.pontosSemana, "semanaRef" to semana,
        )), "ranking")
        Dados.ranking.removeAll { it.ra == a.ra }
        Dados.ranking += PosicaoRanking(a.ra, a.nome, a.pontosSemana)
        notificar(a.ra, "Treino concluído 💪",
            "Você completou o ${treino.nome} e ganhou +${Dados.PONTOS_POR_TREINO} pontos.")
    }

    /** RF20.2 */
    fun marcarComoLida(n: Notificacao) {
        n.lida = true
        gravar(alunoRef.collection("notificacoes").document(n.id).update("lida", true), "notificação lida")
    }

    /** RF20.5 */
    fun limparNotificacoes() {
        val lote = db.batch()
        Dados.notificacoes.forEach { lote.delete(alunoRef.collection("notificacoes").document(it.id)) }
        Dados.notificacoes.clear()
        gravar(lote.commit(), "limpar notificações")
    }

    /** RF22.2 — mensagem para o professor responsável. */
    fun enviarMensagem(texto: String) {
        val a = Dados.aluno
        gravar(db.collection("mensagens").add(mapOf(
            "ra" to a.ra, "nomeAluno" to a.nome, "professorUid" to a.professorUid,
            "texto" to texto, "criadaEm" to System.currentTimeMillis(), "remetenteUid" to (auth.uid ?: ""),
        )), "mensagem")
    }

    /** Cria uma notificação para o aluno [ra] (usada pelo app e pelo painel). */
    fun notificar(ra: String, titulo: String, mensagem: String) {
        val ref = db.collection("alunos").document(ra).collection("notificacoes").document()
        val agora = System.currentTimeMillis()
        if (ra == Dados.aluno.ra && Dados.papel == Dados.Papel.ALUNO) {
            Dados.notificacoes.add(0, Notificacao(ref.id, titulo, mensagem, agora, false))
        }
        gravar(ref.set(mapOf("titulo" to titulo, "mensagem" to mensagem, "criadaEm" to agora, "lida" to false)),
            "notificação")
    }

    /**
     * RF39.3 — as regras ativas que dependem só dos dados do aluno são verificadas quando ele entra.
     * Cada aviso é enviado uma vez por ocorrência.
     */
    private fun verificarLembretes(regras: List<Lembrete>) {
        val a = Dados.aluno
        val enviar = mutableListOf<Pair<String, Pair<String, String>>>()
        regras.filter { it.ativo }.forEach { regra ->
            when (regra.nome) {
                "Reavaliação de anamnese" -> {
                    val ultima = runCatching {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).parse(a.parqRespondidoEm)?.time
                    }.getOrNull()
                    val dias = ultima?.let { (System.currentTimeMillis() - it) / 86_400_000 } ?: 0
                    if (dias >= 60 && a.lembretesEnviados[regra.nome] != a.parqRespondidoEm) {
                        enviar += regra.nome to ("Lembrete do professor" to "Não esqueça de reavaliar sua anamnese.")
                        a.lembretesEnviados[regra.nome] = a.parqRespondidoEm
                    }
                }
                "Treino não realizado" -> {
                    val ultimo = a.concluidos.maxOrNull()
                    val dias = ultimo?.let {
                        runCatching { (System.currentTimeMillis() - Dados.formatoData.parse(it)!!.time) / 86_400_000 }
                            .getOrNull()
                    } ?: 0
                    if (dias >= 3 && a.lembretesEnviados[regra.nome] != ultimo) {
                        enviar += regra.nome to ("Hora de voltar a treinar" to "Faz $dias dias que você não treina. Seu treino te espera!")
                        a.lembretesEnviados[regra.nome] = ultimo.orEmpty()
                    }
                }
            }
        }
        if (enviar.isEmpty()) return
        enviar.forEach { (_, msg) -> notificar(a.ra, msg.first, msg.second) }
        gravar(alunoRef.update("lembretesEnviados", a.lembretesEnviados), "lembretes enviados")
    }

    // =====================================================================
    // Gravações do gestor
    // =====================================================================

    /** RF29.5 / RF30.4 — cria ou atualiza o cadastro; trocar o R.A. move o documento. */
    fun salvarAluno(aluno: Aluno, raAntigo: String?) {
        val lote = db.batch()
        val novoRef = db.collection("alunos").document(aluno.ra)
        if (raAntigo != null && raAntigo != aluno.ra) {
            lote.set(novoRef, alunoParaMapa(aluno))
            lote.delete(db.collection("alunos").document(raAntigo))
            lote.delete(db.collection("ras").document(raAntigo))
            lote.delete(db.collection("ranking").document(raAntigo))
        } else if (raAntigo == null) {
            lote.set(novoRef, alunoParaMapa(aluno))
        } else {
            lote.set(novoRef, cadastroParaMapa(aluno), SetOptions.merge())
        }
        lote.set(db.collection("ras").document(aluno.ra), mapOf("email" to aluno.email, "temConta" to aluno.temConta))
        lote.set(db.collection("ranking").document(aluno.ra), mapOf("nome" to aluno.nome), SetOptions.merge())
        gravar(lote.commit(), "aluno")
        contarAlunosPorTreino()
    }

    /** RF30.5 */
    fun excluirAluno(aluno: Aluno) {
        Dados.alunos.remove(aluno)
        val lote = db.batch()
        lote.delete(db.collection("alunos").document(aluno.ra))
        lote.delete(db.collection("ras").document(aluno.ra))
        lote.delete(db.collection("ranking").document(aluno.ra))
        gravar(lote.commit(), "excluir aluno")
        contarAlunosPorTreino()
    }

    /** RF31.7.2 — o aluno recebe o pedido de novo PAR-Q nas notificações. */
    fun solicitarNovoParq(aluno: Aluno) =
        notificar(aluno.ra, "Novo teste de PAR-Q", "Seu professor pediu que você atualize sua anamnese e o PAR-Q.")

    /** RF35.3 / RF36.5 */
    fun salvarTreino(treino: Treino) {
        gravar(db.collection("treinos").document(treino.id.toString()).set(treinoParaMapa(treino)), "treino")
    }

    /** RF37.4 / RF38 */
    fun salvarExercicio(exercicio: Exercicio) {
        gravar(db.collection("exercicios").document(exercicio.id.toString()).set(exercicioParaMapa(exercicio)),
            "exercício")
    }

    /** RF39.2 / RF39.4 */
    fun salvarLembrete(lembrete: Lembrete) {
        val ref = if (lembrete.id.isEmpty()) db.collection("lembretes").document() else
            db.collection("lembretes").document(lembrete.id)
        lembrete.id = ref.id
        gravar(ref.set(mapOf("nome" to lembrete.nome, "condicao" to lembrete.condicao, "ativo" to lembrete.ativo)),
            "lembrete")
    }

    /** RF40.1 / RF40.3 */
    fun salvarGestor() {
        gravar(db.collection("gestores").document(Dados.gestorUid).set(mapOf(
            "nome" to Dados.nomeGestor, "email" to Dados.emailGestor, "avisos" to Dados.avisosPainel,
        ), SetOptions.merge()), "gestor")
    }

    // =====================================================================
    // Conversão entre documentos e modelos
    // =====================================================================

    fun exercicioParaMapa(e: Exercicio) = mapOf(
        "id" to e.id, "nome" to e.nome, "maquina" to e.maquina, "grupo" to e.grupo,
        "setor" to e.setor, "instrucoes" to e.instrucoes,
    )

    private fun paraExercicio(d: DocumentSnapshot): Exercicio? {
        val id = d.getLong("id")?.toInt() ?: d.id.toIntOrNull() ?: return null
        return Exercicio(id, d.getString("nome").orEmpty(), d.getString("maquina").orEmpty(),
            d.getString("grupo").orEmpty(), d.getString("setor").orEmpty(), d.getString("instrucoes").orEmpty())
    }

    fun treinoParaMapa(t: Treino) = mapOf(
        "id" to t.id, "nome" to t.nome, "categoria" to t.categoria, "nivel" to t.nivel,
        "duracaoMin" to t.duracaoMin,
        "itens" to t.itens.map {
            mapOf("exercicioId" to it.exercicio.id, "series" to it.series, "repeticoes" to it.repeticoes,
                "carga" to it.carga, "descansoSeg" to it.descansoSeg)
        },
    )

    private fun paraTreino(d: DocumentSnapshot): Treino? {
        val id = d.getLong("id")?.toInt() ?: d.id.toIntOrNull() ?: return null
        val itens = (d.get("itens") as? List<*>).orEmpty().mapNotNull { bruto ->
            val m = bruto as? Map<*, *> ?: return@mapNotNull null
            val ex = Dados.exercicios.firstOrNull { it.id == (m["exercicioId"] as? Number)?.toInt() }
                ?: return@mapNotNull null
            ItemTreino(ex, (m["series"] as? Number)?.toInt() ?: 3, (m["repeticoes"] as? Number)?.toInt() ?: 12,
                (m["carga"] as? Number)?.toInt() ?: 0, (m["descansoSeg"] as? Number)?.toInt() ?: 60)
        }.toMutableList()
        return Treino(id, d.getString("nome").orEmpty(), d.getString("categoria").orEmpty(),
            d.getString("nivel").orEmpty(), d.getLong("duracaoMin")?.toInt() ?: 0, 0, itens)
    }

    private fun paraLembrete(d: DocumentSnapshot) =
        Lembrete(d.id, d.getString("nome").orEmpty(), d.getString("condicao").orEmpty(), d.getBoolean("ativo") == true)

    fun cadastroParaMapa(a: Aluno) = mapOf(
        "nome" to a.nome, "ra" to a.ra, "email" to a.email, "status" to a.status, "turma" to a.turma,
        "treinoId" to a.treinoId, "desde" to a.desde, "professorUid" to a.professorUid, "plano" to a.plano,
    )

    fun saudeParaMapa(a: Aluno) = mapOf(
        "pesoKg" to a.pesoKg, "alturaCm" to a.alturaCm, "lesao" to a.lesao, "objetivo" to a.objetivo,
        "respostasParq" to a.respostasParq, "parqRespondidoEm" to a.parqRespondidoEm,
        "cintura" to a.cintura, "quadril" to a.quadril, "braco" to a.braco, "coxa" to a.coxa, "gordura" to a.gordura,
        "medicoes" to a.medicoes.map {
            mapOf("data" to it.data, "cintura" to it.cintura, "quadril" to it.quadril, "braco" to it.braco,
                "coxa" to it.coxa, "gordura" to it.gordura)
        },
        "historicoPeso" to a.historicoPeso.map { it.toDouble() },
    )

    fun estatisticasParaMapa(a: Aluno) = mapOf(
        "treinosMes" to a.treinosMes, "mesRef" to a.mesRef, "treinosTotais" to a.treinosTotais,
        "pontos" to a.pontos, "pontosSemana" to a.pontosSemana, "semanaRef" to a.semanaRef,
        "posicaoSemanaPassada" to a.posicaoSemanaPassada, "cargaEvoluidaKg" to a.cargaEvoluidaKg,
        "concluidos" to a.concluidos,
        "historicoCarga" to a.historicoCarga.mapValues { (_, v) -> v.map { it.toDouble() } },
        "cargas" to a.cargas.mapKeys { it.key.toString() },
        "ultimaCarga" to a.ultimaCarga.mapKeys { it.key.toString() },
        "lembretesEnviados" to a.lembretesEnviados,
    )

    fun alunoParaMapa(a: Aluno): Map<String, Any?> =
        cadastroParaMapa(a) + saudeParaMapa(a) + estatisticasParaMapa(a) +
            mapOf("uid" to a.uid, "onboardingPendente" to a.onboardingPendente)

    @Suppress("UNCHECKED_CAST")
    fun paraAluno(d: DocumentSnapshot): Aluno? {
        if (!d.exists()) return null
        fun s(k: String) = d.getString(k).orEmpty()
        fun i(k: String) = d.getLong(k)?.toInt() ?: 0
        fun numeros(k: String) = (d.get(k) as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toFloat() }
        fun mapaInt(k: String) = (d.get(k) as? Map<String, Any?>).orEmpty()
            .mapNotNull { (c, v) -> c.toIntOrNull()?.let { id -> (v as? Number)?.toInt()?.let { id to it } } }
            .toMap().toMutableMap()
        val plano = (d.get("plano") as? List<*>).orEmpty().map { it?.toString().orEmpty() }
        return Aluno(
            nome = s("nome"), ra = d.id, email = s("email"), status = s("status"), turma = s("turma"),
            treinoId = d.getLong("treinoId")?.toInt(), desde = s("desde"), professorUid = s("professorUid"),
            uid = s("uid"), onboardingPendente = d.getBoolean("onboardingPendente") == true,
            plano = MutableList(7) { plano.getOrElse(it) { "" } },
            pesoKg = d.getDouble("pesoKg") ?: 0.0, alturaCm = i("alturaCm"), lesao = s("lesao"),
            objetivo = s("objetivo"),
            respostasParq = (d.get("respostasParq") as? List<*>).orEmpty().map { it as? Boolean }
                .let { r -> MutableList(3) { r.getOrNull(it) } },
            parqRespondidoEm = s("parqRespondidoEm"),
            cintura = s("cintura"), quadril = s("quadril"), braco = s("braco"), coxa = s("coxa"), gordura = s("gordura"),
            medicoes = (d.get("medicoes") as? List<*>).orEmpty().mapNotNull { m ->
                (m as? Map<String, Any?>)?.let {
                    Medicao(it["data"]?.toString().orEmpty(), it["cintura"]?.toString().orEmpty(),
                        it["quadril"]?.toString().orEmpty(), it["braco"]?.toString().orEmpty(),
                        it["coxa"]?.toString().orEmpty(), it["gordura"]?.toString().orEmpty())
                }
            }.toMutableList(),
            historicoPeso = numeros("historicoPeso").toMutableList(),
            treinosMes = i("treinosMes"), mesRef = s("mesRef"), treinosTotais = i("treinosTotais"),
            pontos = i("pontos"), pontosSemana = i("pontosSemana"), semanaRef = s("semanaRef"),
            posicaoSemanaPassada = i("posicaoSemanaPassada"), cargaEvoluidaKg = d.getDouble("cargaEvoluidaKg") ?: 0.0,
            concluidos = (d.get("concluidos") as? List<*>).orEmpty().map { it.toString() }.toMutableList(),
            historicoCarga = LinkedHashMap<String, MutableList<Float>>().apply {
                (d.get("historicoCarga") as? Map<String, Any?>).orEmpty().toSortedMap(compareBy { ordemCarga(it) })
                    .forEach { (k, v) -> put(k, (v as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toFloat() }.toMutableList()) }
            },
            cargas = mapaInt("cargas"), ultimaCarga = mapaInt("ultimaCarga"),
            lembretesEnviados = (d.get("lembretesEnviados") as? Map<String, Any?>).orEmpty()
                .mapValues { it.value.toString() }.toMutableMap(),
        )
    }

    /** As abas da evolução de carga aparecem sempre nesta ordem. */
    private fun ordemCarga(nome: String) = listOf("Leg Press", "Supino", "Puxada").indexOf(nome).let {
        if (it < 0) 99 else it
    }
}
