package br.edu.unifor.uniforfit.data

import android.graphics.Bitmap
import android.net.Uri
import br.edu.unifor.uniforfit.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Foto de progresso vinda da câmera ([bitmap]) ou da galeria ([uri]). */
data class FotoProgresso(val bitmap: Bitmap? = null, val uri: Uri? = null)

/**
 * Estado da sessão em memória. É carregado do Firestore pelo [Banco] no login e
 * cada alteração feita nas telas é gravada de volta no Firestore pelo próprio [Banco].
 */
object Dados {

    const val PONTOS_POR_TREINO = 50

    /** Contas criadas pelo [Semeador] na primeira execução. */
    const val RA_DEMO = "2201234567"
    const val SENHA_DEMO = "12345678"
    const val EMAIL_GESTOR = "professor@unifor.br"
    const val SENHA_GESTOR = "12345678"

    // ---------- Sessão ----------
    var papel: Papel? = null
    val sessaoCarregada get() = papel != null

    enum class Papel { ALUNO, GESTOR }

    /** Aluno logado (no app do aluno). */
    var aluno: Aluno = Aluno("", "", "", "", "", null, "", "")

    // Atalhos usados pelas telas do aluno
    val nomeAluno get() = aluno.nome
    val raAluno get() = aluno.ra
    var pesoKg: Double
        get() = aluno.pesoKg
        set(v) { aluno.pesoKg = v }
    var alturaCm: Int
        get() = aluno.alturaCm
        set(v) { aluno.alturaCm = v }
    var lesao: String
        get() = aluno.lesao
        set(v) { aluno.lesao = v }
    var objetivo: String
        get() = aluno.objetivo
        set(v) { aluno.objetivo = v }
    val respostasParq get() = aluno.respostasParq
    var parqRespondidoEm: String
        get() = aluno.parqRespondidoEm
        set(v) { aluno.parqRespondidoEm = v }
    var cintura: String
        get() = aluno.cintura
        set(v) { aluno.cintura = v }
    var quadril: String
        get() = aluno.quadril
        set(v) { aluno.quadril = v }
    var braco: String
        get() = aluno.braco
        set(v) { aluno.braco = v }
    var coxa: String
        get() = aluno.coxa
        set(v) { aluno.coxa = v }
    var gordura: String
        get() = aluno.gordura
        set(v) { aluno.gordura = v }
    val historicoPeso get() = aluno.historicoPeso
    val historicoCarga get() = aluno.historicoCarga
    val ultimaCargaRegistrada get() = aluno.ultimaCarga
    val treinosMes get() = if (aluno.mesRef == mesAtual()) aluno.treinosMes else 0
    val cargaEvoluidaKg get() = aluno.cargaEvoluidaKg
    val treinosTotais get() = aluno.treinosTotais
    val pontos get() = aluno.pontos
    val primeiroNome get() = aluno.nome.substringBefore(" ")

    /** Nome do professor responsável pelo aluno logado (RF22.1). */
    var professorDoAluno = ""

    val fotosProgresso = mutableListOf<FotoProgresso>()

    // ---------- Catálogo ----------
    val exercicios = mutableListOf<Exercicio>()
    val treinos = mutableListOf<Treino>()

    fun exercicio(id: Int) = exercicios.first { it.id == id }
    fun proximoIdExercicio() = (exercicios.maxOfOrNull { it.id } ?: 0) + 1
    fun treino(id: Int?) = treinos.firstOrNull { it.id == id }
    fun proximoIdTreino() = (treinos.maxOfOrNull { it.id } ?: 0) + 1

    // ---------- Minha semana (RF10) ----------
    private val nomesDias = listOf(
        "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo",
    )
    private val letras = listOf("S", "T", "Q", "Q", "S", "S", "D")

    /** Posição de hoje na semana (segunda = 0). */
    val INDICE_HOJE: Int
        get() = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    /** Data (yyyy-MM-dd) do dia [indice] da semana atual. */
    fun dataDoDia(indice: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, indice - INDICE_HOJE)
        return formatoData.format(c.time)
    }

    val treinoDeHojeConcluido get() = hoje() in aluno.concluidos

    val semana: List<DiaSemana>
        get() = List(7) { i ->
            val item = aluno.plano.getOrElse(i) { "" }
            val treino = if (item.startsWith("t:")) treino(item.removePrefix("t:").toIntOrNull()) else null
            when {
                item == "descanso" -> DiaSemana(nomesDias[i], letras[i], null, StatusDia.DESCANSO,
                    "Dia de recuperação", "Alongamento leve recomendado")
                treino == null -> DiaSemana(nomesDias[i], letras[i], null, null,
                    "Nenhum treino atribuído", "Seu professor ainda não montou este dia")
                dataDoDia(i) in aluno.concluidos -> DiaSemana(nomesDias[i], letras[i], treino, StatusDia.CONCLUIDO)
                i == INDICE_HOJE -> DiaSemana(nomesDias[i], letras[i], treino, StatusDia.HOJE)
                else -> DiaSemana(nomesDias[i], letras[i], treino, StatusDia.PLANEJADO)
            }
        }

    val diaDeHoje get() = semana[INDICE_HOJE]

    // ---------- Evolução ----------
    fun chaveHistorico(exercicio: Exercicio) =
        historicoCarga.keys.firstOrNull { exercicio.nome.startsWith(it) }

    fun imc(peso: Double = pesoKg, alturaCm: Int = this.alturaCm): Double {
        if (alturaCm <= 0) return 0.0
        val metros = alturaCm / 100.0
        return peso / (metros * metros)
    }

    /** Classificação da OMS e a cor do selo exibido na Calculadora de IMC (RF18.2). */
    fun classificacaoImc(imc: Double): Pair<String, Int> = when {
        imc < 18.5 -> "Abaixo do peso" to R.color.laranja
        imc < 25.0 -> "Peso normal" to R.color.verde
        imc < 30.0 -> "Sobrepeso" to R.color.laranja
        else -> "Obesidade" to R.color.vermelho
    }

    // ---------- Ranking (RF19) ----------
    val ranking = mutableListOf<PosicaoRanking>()

    /** Ranking ordenado; só valem os pontos da semana atual. */
    fun rankingOrdenado(): List<PosicaoRanking> = ranking.sortedWith(
        compareByDescending<PosicaoRanking> { it.pontosSemana }.thenBy { it.nome },
    )

    val posicaoRanking get() = rankingOrdenado().indexOfFirst { it.ra == aluno.ra } + 1

    // ---------- Notificações (mais recentes primeiro — RF20.3) ----------
    val notificacoes = mutableListOf<Notificacao>()

    val perguntasParq = listOf(
        "Algum médico já disse que você possui um problema de coração?",
        "Você sente dores no peito quando pratica atividade física?",
        "Você perde o equilíbrio devido a tontura ou já perdeu a consciência?",
    )

    // ---------- Painel do Gestor ----------
    var gestorUid = ""
    var nomeGestor = ""
    var emailGestor = ""
    val alunos = mutableListOf<Aluno>()
    val lembretes = mutableListOf<Lembrete>()
    val avisosPainel = linkedMapOf<String, Boolean>()

    /** Medidas atuais e a variação em relação à medição anterior (RF31.8.1). */
    fun medidasDe(aluno: Aluno): List<Medida> {
        val atual = aluno.medicoes.firstOrNull()
        val anterior = aluno.medicoes.getOrNull(1)
        fun linha(nome: String, unidade: String, v: (Medicao) -> String, menorMelhor: Boolean): Medida {
            val agora = atual?.let(v).orEmpty()
            val antes = anterior?.let(v)?.toDoubleOrNull()
            val n = agora.toDoubleOrNull()
            val dif = if (n != null && antes != null) n - antes else null
            val texto = when {
                dif == null -> "—"
                dif > 0 -> "+" + formatarNumero(dif) + unidade
                dif < 0 -> "-" + formatarNumero(-dif) + unidade
                else -> "0$unidade"
            }
            val melhorou = dif == null || (if (menorMelhor) dif <= 0 else dif >= 0)
            val valor = if (agora.isEmpty()) "—" else if (unidade == "%") "$agora%" else "$agora cm"
            return Medida(nome, valor, texto, melhorou)
        }
        return listOf(
            linha("Cintura", "cm", { it.cintura }, true),
            linha("Quadril", "cm", { it.quadril }, true),
            linha("Braço", "cm", { it.braco }, false),
            linha("Coxa", "cm", { it.coxa }, false),
            linha("% Gordura", "%", { it.gordura }, true),
        )
    }

    fun historicoMedicoesDe(aluno: Aluno) = aluno.medicoes.map {
        "${it.data}  ·  ${it.cintura} · ${it.quadril} · ${it.braco} · ${it.coxa} · ${it.gordura.ifEmpty { "—" }}%"
    }

    val opcoesStatus = listOf("Ativo", "Pendente", "Inativo")
    val opcoesTurma = listOf("Musculação", "Funcional", "Cardio")
    val opcoesCategoria = listOf("Musculação", "Cardio", "Funcional")
    val opcoesNivel = listOf("Iniciante", "Intermediário", "Avançado", "Todos os níveis")
    val opcoesGrupo = listOf("Pernas", "Costas", "Peito", "Braços", "Ombros", "Cardio")
    val opcoesSetor = listOf(
        "Setor de Pernas", "Setor de Costas", "Setor de Peito", "Setor de Polias", "Setor de Cardio", "Área Livre",
    )

    // ---------- Datas ----------
    val formatoData = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun hoje(): String = formatoData.format(Date())
    fun mesAtual(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    fun semanaAtual(): String = dataDoDia(0)
    fun dataBr(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

    private fun formatarNumero(v: Double) =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale("pt", "BR"), "%.1f", v)

    /** Limpa tudo ao sair da conta. */
    fun limpar() {
        papel = null
        aluno = Aluno("", "", "", "", "", null, "", "")
        professorDoAluno = ""
        fotosProgresso.clear()
        exercicios.clear()
        treinos.clear()
        ranking.clear()
        notificacoes.clear()
        gestorUid = ""
        nomeGestor = ""
        emailGestor = ""
        alunos.clear()
        lembretes.clear()
        avisosPainel.clear()
    }
}
