package br.edu.unifor.uniforfit.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Prepara o banco na primeira vez que o app abre (só se `config/seed` não existir):
 * cria a conta do gestor (professor@unifor.br), os alunos matriculados, o catálogo de
 * exercícios e treinos, os lembretes e a conta do aluno de demonstração (R.A. 2201234567).
 * As senhas de demonstração estão em [Dados] e no NAVEGACAO.md.
 */
object Semeador {

    private const val TAG = "Semeador"

    suspend fun semearSeNecessario() {
        val db = Banco.db
        val auth = Banco.auth
        val jaFeito = try {
            db.collection("config").document("seed").get().await().exists()
        } catch (e: Exception) {
            null // regras antigas: só usuários logados leem; verifica de novo depois de entrar
        }
        if (jaFeito == true) return
        try {
            val gestorUid = entrarOuCriar(Dados.EMAIL_GESTOR, Dados.SENHA_GESTOR)
            if (jaFeito == null && db.collection("config").document("seed").get().await().exists()) return
            Log.i(TAG, "banco vazio: criando dados iniciais")
            db.collection("gestores").document(gestorUid).set(mapOf(
                "nome" to "Prof. Carlos Andrade", "email" to Dados.EMAIL_GESTOR,
                "avisos" to linkedMapOf(
                    "Anamnese pendente" to true, "Novo aluno cadastrado" to true,
                    "Mensagem de aluno" to true, "Treino não realizado" to false,
                ),
            )).await()

            val lote = db.batch()
            val exercicios = exercicios()
            exercicios.forEach {
                lote.set(db.collection("exercicios").document(it.id.toString()), Banco.exercicioParaMapa(it))
            }
            treinos(exercicios).forEach {
                lote.set(db.collection("treinos").document(it.id.toString()), Banco.treinoParaMapa(it))
            }
            listOf(
                Lembrete("1", "Reavaliação de anamnese", "A cada 60 dias · Todos os alunos", true),
                Lembrete("2", "Treino não realizado", "Após 3 dias sem check-in", true),
                Lembrete("3", "Renovação de matrícula", "7 dias antes do vencimento", true),
                Lembrete("4", "Aniversário do aluno", "No dia do aniversário", false),
            ).forEach {
                lote.set(db.collection("lembretes").document(it.id),
                    mapOf("nome" to it.nome, "condicao" to it.condicao, "ativo" to it.ativo))
            }
            val semana = Dados.semanaAtual()
            alunos(gestorUid).forEach { a ->
                lote.set(db.collection("alunos").document(a.ra), Banco.alunoParaMapa(a))
                lote.set(db.collection("ras").document(a.ra), mapOf("email" to a.email, "temConta" to false))
                lote.set(db.collection("ranking").document(a.ra),
                    mapOf("nome" to a.nome, "pontosSemana" to a.pontosSemana, "semanaRef" to semana))
            }
            val agora = System.currentTimeMillis()
            val hora = 3_600_000L
            listOf(
                Triple("Hora do treino!", "Seu Treino C — Pernas te espera hoje.", agora - 2 * hora) to false,
                Triple("Novo recorde 🎉", "Você bateu recorde no Leg Press: 80kg.", agora - 26 * hora) to false,
                Triple("Lembrete do professor", "Não esqueça de reavaliar sua anamnese.", agora - 72 * hora) to true,
                Triple("Você subiu no ranking", "Parabéns, você foi para #4 esta semana.", agora - 120 * hora) to true,
            ).forEach { (n, lida) ->
                lote.set(db.collection("alunos").document(Dados.RA_DEMO).collection("notificacoes").document(),
                    mapOf("titulo" to n.first, "mensagem" to n.second, "criadaEm" to n.third, "lida" to lida))
            }
            lote.set(db.collection("config").document("seed"), mapOf("criadoEm" to agora))
            lote.commit().await()

            // Conta do aluno de demonstração, ligada ao cadastro como no primeiro acesso.
            val alunoUid = entrarOuCriar("giuseppe@edu.unifor.br", Dados.SENHA_DEMO)
            db.collection("alunos").document(Dados.RA_DEMO)
                .set(mapOf("uid" to alunoUid), SetOptions.merge()).await()
            db.collection("ras").document(Dados.RA_DEMO)
                .set(mapOf("email" to "giuseppe@edu.unifor.br", "temConta" to true)).await()
            Log.i(TAG, "dados iniciais criados")
        } catch (e: Exception) {
            Log.e(TAG, "falha ao criar os dados iniciais", e)
        } finally {
            auth.signOut()
        }
    }

    private suspend fun entrarOuCriar(email: String, senha: String): String {
        val auth = Banco.auth
        return try {
            auth.createUserWithEmailAndPassword(email, senha).await().user!!.uid
        } catch (e: FirebaseAuthUserCollisionException) {
            auth.signInWithEmailAndPassword(email, senha).await().user!!.uid
        }
    }

    // ---------- Dados de exemplo (os mesmos do protótipo) ----------

    private fun exercicios() = listOf(
        Exercicio(1, "Leg Press 45°", "Máquina 12 — Leg Press", "Pernas", "Setor de Pernas",
            "Ajuste o encosto, posicione os pés na plataforma na largura dos ombros e empurre sem travar os joelhos."),
        Exercicio(2, "Cadeira Extensora", "Máquina 14 — Extensora", "Pernas", "Setor de Pernas",
            "Apoie as costas no encosto, alinhe o joelho ao eixo da máquina e estenda as pernas de forma controlada."),
        Exercicio(3, "Cadeira Flexora", "Máquina 15 — Flexora", "Pernas", "Setor de Pernas",
            "Deite-se com o rolo logo acima dos calcanhares e flexione os joelhos sem tirar o quadril do banco."),
        Exercicio(4, "Agachamento Livre", "Área Livre — Squat Rack 2", "Pernas", "Área Livre",
            "Barra apoiada no trapézio e pés na largura dos ombros; desça com o tronco firme até as coxas ficarem paralelas ao chão."),
        Exercicio(5, "Supino Reto", "Máquina 3 — Supino", "Peito", "Setor de Peito",
            "Deite no banco com os pés no chão, desça a barra até a linha do peito e empurre estendendo os braços."),
        Exercicio(6, "Crucifixo Máquina", "Máquina 5 — Peck Deck", "Peito", "Setor de Peito",
            "Mantenha os cotovelos levemente flexionados e aproxime os braços à frente do peito, voltando devagar."),
        Exercicio(7, "Tríceps Corda", "Polia 2 — Cross Over", "Braços", "Setor de Polias",
            "Cotovelos colados ao corpo; estenda os braços puxando a corda para baixo e abra as mãos no final."),
        Exercicio(8, "Puxada Frontal", "Máquina 8 — Pulldown", "Costas", "Setor de Costas",
            "Segure a barra com pegada aberta e puxe até a altura do queixo, levando os cotovelos para baixo."),
        Exercicio(9, "Remada Baixa", "Máquina 9 — Remada", "Costas", "Setor de Costas",
            "Coluna neutra; puxe o triângulo em direção ao abdômen aproximando as escápulas."),
        Exercicio(10, "Rosca Direta", "Área Livre — Halteres", "Braços", "Área Livre",
            "Cotovelos fixos ao lado do corpo; flexione os braços sem balançar o tronco."),
        Exercicio(11, "Esteira", "Cardio — Esteira 4", "Cardio", "Setor de Cardio",
            "Alterne 1 minuto de corrida forte com 1 minuto de caminhada."),
        Exercicio(12, "Bicicleta Ergométrica", "Cardio — Bike 2", "Cardio", "Setor de Cardio",
            "Pedale em ritmo moderado mantendo a coluna ereta."),
    )

    private fun treinos(ex: List<Exercicio>): List<Treino> {
        fun e(id: Int) = ex.first { it.id == id }
        return listOf(
            Treino(1, "Treino A — Peito e Tríceps", "Musculação", "Intermediário", 50, 0, mutableListOf(
                ItemTreino(e(5), 4, 10, 40), ItemTreino(e(6), 3, 12, 30), ItemTreino(e(7), 3, 12, 20))),
            Treino(2, "Treino B — Costas e Bíceps", "Musculação", "Intermediário", 45, 0, mutableListOf(
                ItemTreino(e(8), 4, 10, 50), ItemTreino(e(9), 3, 12, 45), ItemTreino(e(10), 3, 12, 12))),
            Treino(3, "Treino C — Pernas", "Musculação", "Intermediário", 55, 0, mutableListOf(
                ItemTreino(e(1), 4, 12, 80), ItemTreino(e(2), 3, 15, 40), ItemTreino(e(3), 3, 15, 35),
                ItemTreino(e(4), 4, 10, 60))),
            Treino(4, "Cardio/HIIT", "Cardio", "Todos os níveis", 20, 0, mutableListOf(
                ItemTreino(e(11), 5, 1, 0, 30), ItemTreino(e(12), 3, 1, 0, 30))),
        )
    }

    /** Semana do protótipo: A, B, C, descanso, HIIT, A e domingo sem treino. */
    private fun plano() = mutableListOf("t:1", "t:2", "t:3", "descanso", "t:4", "t:1", "")

    private fun alunos(gestorUid: String): List<Aluno> {
        val semana = Dados.semanaAtual()
        val mes = Dados.mesAtual()
        val hoje = Dados.INDICE_HOJE
        // Dias desta semana, anteriores a hoje, que tinham treino: aparecem como concluídos.
        val concluidos = (0 until hoje).filter { plano()[it].startsWith("t:") }.map { Dados.dataDoDia(it) }
        val antigo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -10) }
        fun base(nome: String, ra: String, status: String, turma: String, treino: Int, desde: String,
                 prof: Boolean, pontos: Int) = Aluno(
            nome = nome, ra = ra, email = "${nome.substringBefore(" ").lowercase()}@edu.unifor.br".replace("ã", "a"),
            status = status, turma = turma, treinoId = treino, desde = desde,
            professorUid = if (prof) gestorUid else "", plano = plano(),
            pontos = pontos, pontosSemana = pontos, semanaRef = semana, mesRef = mes,
        )
        val giuseppe = base("Giuseppe Chinelate", Dados.RA_DEMO, "Ativo", "Musculação", 3, "mar/2026", true, 740).apply {
            pesoKg = 72.0; alturaCm = 175; objetivo = "Hipertrofia"
            respostasParq[0] = false; respostasParq[1] = false; respostasParq[2] = false
            parqRespondidoEm = "12/03/2026"
            cintura = "82"; quadril = "98"; braco = "34"; coxa = "56"; gordura = "18"
            medicoes += Medicao("10/09/2026", "82", "98", "34", "56", "18")
            medicoes += Medicao("10/06/2026", "85", "99", "33", "54", "20")
            medicoes += Medicao("10/03/2026", "88", "101", "32", "52", "22")
            historicoPeso += listOf(76.2f, 75.6f, 75.0f, 74.1f, 73.4f, 72.6f, 72.0f)
            treinosMes = 12; treinosTotais = 32; cargaEvoluidaKg = 3.5; posicaoSemanaPassada = 6
            this.concluidos += concluidos
            this.concluidos += Dados.formatoData.format(antigo.time)
            historicoCarga["Leg Press"] = mutableListOf(40f, 45f, 50f, 55f, 60f, 70f, 80f)
            historicoCarga["Supino"] = mutableListOf(30f, 32f, 35f, 35f, 38f, 40f, 40f)
            historicoCarga["Puxada"] = mutableListOf()
            ultimaCarga[1] = 75
        }
        fun saude(a: Aluno, peso: Double, altura: Int, objetivo: String, cintura: String, quadril: String,
                  braco: String, coxa: String, gordura: String) = a.apply {
            pesoKg = peso; alturaCm = altura; this.objetivo = objetivo
            respostasParq[0] = false; respostasParq[1] = false; respostasParq[2] = false
            parqRespondidoEm = "05/08/2026"
            this.cintura = cintura; this.quadril = quadril; this.braco = braco; this.coxa = coxa; this.gordura = gordura
            medicoes += Medicao("05/08/2026", cintura, quadril, braco, coxa, gordura)
            historicoPeso += peso.toFloat()
            historicoCarga["Leg Press"] = mutableListOf(); historicoCarga["Supino"] = mutableListOf()
            historicoCarga["Puxada"] = mutableListOf()
        }
        return listOf(
            giuseppe,
            saude(base("Marina Costa", "2201234568", "Ativo", "Musculação", 1, "fev/2026", true, 980),
                61.0, 165, "Definição", "68", "94", "27", "52", "24"),
            saude(base("Lucas Pereira", "2201234569", "Ativo", "Funcional", 4, "jan/2026", false, 1240),
                80.0, 182, "Condicionamento", "86", "100", "36", "58", "17"),
            base("Beatriz Lima", "2201234570", "Pendente", "Musculação", 2, "set/2026", true, 520),
            saude(base("Rafael Souza", "2201234571", "Ativo", "Cardio", 4, "abr/2026", false, 870),
                90.0, 178, "Emagrecimento", "95", "104", "35", "60", "26"),
            saude(base("João Mendes", "2201234572", "Ativo", "Musculação", 1, "mai/2026", false, 410),
                70.0, 172, "Hipertrofia", "80", "96", "33", "55", "19"),
        )
    }
}
