package br.edu.unifor.uniforfit.data

data class Exercicio(
    val id: Int,
    var nome: String,
    /** Nomenclatura padrão da academia (RF11.5), ex.: "Máquina 12 — Leg Press". */
    var maquina: String,
    var grupo: String,
    var setor: String,
    var instrucoes: String,
)

/** Exercício dentro de um treino, com a prescrição de séries, repetições, carga e descanso. */
data class ItemTreino(
    val exercicio: Exercicio,
    var series: Int,
    var repeticoes: Int,
    var carga: Int,
    var descansoSeg: Int = 60,
) {
    /** Exercícios de cardio não têm carga: mostram só séries e repetições. */
    val cargaTexto get() = if (carga > 0) "${carga}kg" else "—"
    val prescricao get() = "$series séries · $repeticoes reps · $cargaTexto"
    val prescricaoCurta get() = "$series×$repeticoes · $cargaTexto"
}

data class Treino(
    val id: Int,
    var nome: String,
    var categoria: String,
    var nivel: String,
    var duracaoMin: Int,
    /** Quantidade de alunos com este treino atribuído (calculada a partir dos alunos). */
    var alunos: Int,
    val itens: MutableList<ItemTreino>,
) {
    val resumo get() = "${itens.size} exercícios · ~$duracaoMin min"
}

enum class StatusDia(val rotulo: String) {
    CONCLUIDO("CONCLUÍDO"),
    HOJE("HOJE"),
    PLANEJADO("PLANEJADO"),
    DESCANSO("DESCANSO"),
}

/** Dia da tela "Minha semana" (RF10). Sem [treino] e sem [status] = nenhum treino atribuído. */
data class DiaSemana(
    val nome: String,
    val letra: String,
    val treino: Treino?,
    val status: StatusDia?,
    val titulo: String = treino?.nome.orEmpty(),
    val descricao: String = treino?.resumo.orEmpty(),
)

/** Uma medição corporal completa, usada na ficha do aluno (RF31.8). */
data class Medicao(
    val data: String,
    val cintura: String,
    val quadril: String,
    val braco: String,
    val coxa: String,
    val gordura: String,
)

/**
 * Aluno matriculado. O documento fica em `alunos/{ra}` no Firestore e reúne o cadastro
 * (mantido pelo gestor), os dados de saúde (anamnese, PAR-Q e medidas) e os totais de treino.
 */
data class Aluno(
    var nome: String,
    var ra: String,
    var email: String,
    var status: String,
    var turma: String,
    var treinoId: Int?,
    val desde: String,
    /** uid do gestor responsável (RF40.2). */
    var professorUid: String,
    /** uid da conta criada no primeiro acesso; vazio enquanto o aluno não tem conta. */
    var uid: String = "",
    var onboardingPendente: Boolean = false,
    /** Plano semanal: 7 posições (segunda a domingo) com "t:<id do treino>", "descanso" ou "". */
    var plano: MutableList<String> = MutableList(7) { "" },

    // Saúde
    var pesoKg: Double = 0.0,
    var alturaCm: Int = 0,
    var lesao: String = "",
    var objetivo: String = "",
    val respostasParq: MutableList<Boolean?> = mutableListOf(null, null, null),
    var parqRespondidoEm: String = "",
    var cintura: String = "",
    var quadril: String = "",
    var braco: String = "",
    var coxa: String = "",
    var gordura: String = "",
    val medicoes: MutableList<Medicao> = mutableListOf(),
    val historicoPeso: MutableList<Float> = mutableListOf(),

    // Treinos e ranking
    var treinosMes: Int = 0,
    var mesRef: String = "",
    var treinosTotais: Int = 0,
    var pontos: Int = 0,
    var pontosSemana: Int = 0,
    var semanaRef: String = "",
    var posicaoSemanaPassada: Int = 0,
    var cargaEvoluidaKg: Double = 0.0,
    /** Datas (yyyy-MM-dd) em que o aluno concluiu um treino. */
    val concluidos: MutableList<String> = mutableListOf(),
    /** Carga por sessão nos exercícios acompanhados na evolução (RF16). */
    val historicoCarga: LinkedHashMap<String, MutableList<Float>> = linkedMapOf(),
    /** Carga personalizada pelo aluno, por id de exercício (RF13). */
    val cargas: MutableMap<Int, Int> = mutableMapOf(),
    /** Carga anterior à atual, por id de exercício (RF13.2). */
    val ultimaCarga: MutableMap<Int, Int> = mutableMapOf(),
    /** Lembretes automáticos já enviados (nome da regra → data), para não repetir. */
    val lembretesEnviados: MutableMap<String, String> = mutableMapOf(),
) {
    val temConta get() = uid.isNotEmpty()
}

data class Lembrete(var id: String, var nome: String, var condicao: String, var ativo: Boolean)

data class Notificacao(
    val id: String,
    val titulo: String,
    val mensagem: String,
    val criadaEm: Long,
    var lida: Boolean,
) {
    /** Tempo decorrido desde o recebimento (RF20.1), ex.: "há 2h", "há 1 dia". */
    val tempo: String
        get() {
            val minutos = (System.currentTimeMillis() - criadaEm) / 60_000
            return when {
                minutos < 1 -> "agora"
                minutos < 60 -> "há ${minutos}min"
                minutos < 60 * 24 -> "há ${minutos / 60}h"
                minutos < 60 * 48 -> "há 1 dia"
                else -> "há ${minutos / (60 * 24)} dias"
            }
        }
}

data class Medida(val nome: String, val valor: String, val variacao: String, val melhorou: Boolean)

/** Linha do ranking da semana (RF19), em `ranking/{ra}`. */
data class PosicaoRanking(val ra: String, val nome: String, val pontosSemana: Int)
