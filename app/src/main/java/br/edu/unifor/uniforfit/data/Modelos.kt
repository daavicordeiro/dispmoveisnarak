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

data class Aluno(
    var nome: String,
    var ra: String,
    var email: String,
    var status: String,
    var turma: String,
    var treino: String,
    val desde: String,
    val doProfessorLogado: Boolean,
)

data class Lembrete(var nome: String, var condicao: String, var ativo: Boolean)

data class Notificacao(val titulo: String, val mensagem: String, val tempo: String, var lida: Boolean)

data class Medida(val nome: String, val valor: String, val variacao: String, val melhorou: Boolean)
