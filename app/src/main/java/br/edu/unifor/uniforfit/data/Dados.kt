package br.edu.unifor.uniforfit.data

import android.graphics.Bitmap
import android.net.Uri
import br.edu.unifor.uniforfit.R

/** Foto de progresso vinda da câmera ([bitmap]) ou da galeria ([uri]). */
data class FotoProgresso(val bitmap: Bitmap? = null, val uri: Uri? = null)

/**
 * Dados de demonstração em memória (o app ainda não tem back-end).
 * Os valores seguem os exemplos do protótipo no Figma.
 */
object Dados {

    // ---------- Credenciais de demonstração ----------
    const val RA_DEMO = "2201234567"
    const val SENHA_DEMO = "12345678"
    const val EMAIL_GESTOR = "professor@unifor.br"
    const val SENHA_GESTOR = "12345678"

    /** R.A. → senha. O "Primeiro acesso" cadastra novas contas aqui. */
    val contas = mutableMapOf(RA_DEMO to SENHA_DEMO)

    /** Contas criadas que ainda não concluíram anamnese, PAR-Q e medidas. */
    val onboardingPendente = mutableSetOf<String>()

    fun loginAlunoValido(ra: String, senha: String) = contas[ra] == senha

    /** Troca o aluno logado (o restante dos dados de demonstração é compartilhado). */
    fun entrarComo(ra: String) {
        raAluno = ra
        alunos.firstOrNull { it.ra == ra }?.let { nomeAluno = it.nome }
    }

    fun loginGestorValido(email: String, senha: String) =
        email.equals(EMAIL_GESTOR, ignoreCase = true) && senha == SENHA_GESTOR

    /** RF03.6 — só R.A. de alunos matriculados podem criar conta. */
    fun raMatriculado(ra: String) = alunos.any { it.ra == ra }

    fun emailDoRa(ra: String) = alunos.firstOrNull { it.ra == ra }?.email

    // ---------- Aluno logado ----------
    var nomeAluno = "Giuseppe Chinelate"
    var raAluno = RA_DEMO
    var pesoKg = 72.0
    var alturaCm = 175
    var lesao = ""
    var objetivo = "Hipertrofia"
    val respostasParq = mutableListOf<Boolean?>(false, false, false)
    var parqRespondidoEm = "12/03/2026"
    var cintura = "82"
    var quadril = "98"
    var braco = "34"
    var coxa = "56"
    var gordura = "18"

    var treinosMes = 12
    var cargaEvoluidaKg = 3.5
    var posicaoRanking = 4
    var treinosTotais = 32
    var pontos = 740
    var treinoDeHojeConcluido = false

    const val PONTOS_POR_TREINO = 50

    val primeiroNome get() = nomeAluno.substringBefore(" ")

    // ---------- Catálogo de exercícios ----------
    val exercicios = mutableListOf(
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

    fun exercicio(id: Int) = exercicios.first { it.id == id }

    fun proximoIdExercicio() = (exercicios.maxOfOrNull { it.id } ?: 0) + 1

    // ---------- Modelos de treino ----------
    val treinos = mutableListOf(
        Treino(1, "Treino A — Peito e Tríceps", "Musculação", "Intermediário", 50, 8, mutableListOf(
            ItemTreino(exercicio(5), 4, 10, 40),
            ItemTreino(exercicio(6), 3, 12, 30),
            ItemTreino(exercicio(7), 3, 12, 20),
        )),
        Treino(2, "Treino B — Costas e Bíceps", "Musculação", "Intermediário", 45, 8, mutableListOf(
            ItemTreino(exercicio(8), 4, 10, 50),
            ItemTreino(exercicio(9), 3, 12, 45),
            ItemTreino(exercicio(10), 3, 12, 12),
        )),
        Treino(3, "Treino C — Pernas", "Musculação", "Intermediário", 55, 12, mutableListOf(
            ItemTreino(exercicio(1), 4, 12, 80),
            ItemTreino(exercicio(2), 3, 15, 40),
            ItemTreino(exercicio(3), 3, 15, 35),
            ItemTreino(exercicio(4), 4, 10, 60),
        )),
        Treino(4, "Cardio/HIIT", "Cardio", "Todos os níveis", 20, 24, mutableListOf(
            ItemTreino(exercicio(11), 5, 1, 0, 30),
            ItemTreino(exercicio(12), 3, 1, 0, 30),
        )),
    )

    fun treino(id: Int) = treinos.firstOrNull { it.id == id }

    fun proximoIdTreino() = (treinos.maxOfOrNull { it.id } ?: 0) + 1

    // ---------- Minha semana (quarta-feira é o "hoje" da demonstração, como no protótipo) ----------
    const val INDICE_HOJE = 2

    val semana: List<DiaSemana>
        get() = listOf(
            DiaSemana("Segunda-feira", "S", treino(1), StatusDia.CONCLUIDO),
            DiaSemana("Terça-feira", "T", treino(2), StatusDia.CONCLUIDO),
            DiaSemana("Quarta-feira", "Q", treino(3),
                if (treinoDeHojeConcluido) StatusDia.CONCLUIDO else StatusDia.HOJE),
            DiaSemana("Quinta-feira", "Q", null, StatusDia.DESCANSO,
                "Dia de recuperação", "Alongamento leve recomendado"),
            DiaSemana("Sexta-feira", "S", treino(4), StatusDia.PLANEJADO),
            DiaSemana("Sábado", "S", treino(1), StatusDia.PLANEJADO),
            DiaSemana("Domingo", "D", null, null,
                "Nenhum treino atribuído", "Seu professor ainda não montou este dia"),
        )

    val diaDeHoje get() = semana[INDICE_HOJE]

    // ---------- Evolução ----------
    /** Carga por sessão (RF16). "Puxada" começa vazia para mostrar o estado "Sem dados ainda". */
    val historicoCarga = linkedMapOf(
        "Leg Press" to mutableListOf(40f, 45f, 50f, 55f, 60f, 70f, 80f),
        "Supino" to mutableListOf(30f, 32f, 35f, 35f, 38f, 40f, 40f),
        "Puxada" to mutableListOf<Float>(),
    )

    /** Carga registrada antes da atual, por exercício (RF13.2). */
    val ultimaCargaRegistrada = mutableMapOf(1 to 75)

    fun chaveHistorico(exercicio: Exercicio) =
        historicoCarga.keys.firstOrNull { exercicio.nome.startsWith(it) }

    /** Peso corporal dos últimos 3 meses (RF17). */
    val historicoPeso = mutableListOf(76.2f, 75.6f, 75.0f, 74.1f, 73.4f, 72.6f, 72.0f)

    val fotosProgresso = mutableListOf<FotoProgresso>()

    fun imc(peso: Double = pesoKg, alturaCm: Int = this.alturaCm): Double {
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

    /** RF15.2 / RF19.2 — grava o treino concluído e soma os pontos do ranking. */
    fun registrarTreinoConcluido(treino: Treino) {
        treinosMes++
        treinosTotais++
        pontos += PONTOS_POR_TREINO
        if (treino.id == diaDeHoje.treino?.id) treinoDeHojeConcluido = true
        treino.itens.forEach { item ->
            chaveHistorico(item.exercicio)?.let { chave -> historicoCarga[chave]?.add(item.carga.toFloat()) }
        }
        notificacoes.add(0, Notificacao("Treino concluído 💪",
            "Você completou o ${treino.nome} e ganhou +$PONTOS_POR_TREINO pontos.", "agora", false))
    }

    // ---------- Notificações (mais recentes primeiro — RF20.3) ----------
    val notificacoes = mutableListOf(
        Notificacao("Hora do treino!", "Seu Treino C — Pernas te espera hoje.", "há 2h", false),
        Notificacao("Novo recorde 🎉", "Você bateu recorde no Leg Press: 80kg.", "há 1 dia", false),
        Notificacao("Lembrete do professor", "Não esqueça de reavaliar sua anamnese.", "há 3 dias", true),
        Notificacao("Você subiu no ranking", "Parabéns, você foi para #4 esta semana.", "há 5 dias", true),
    )

    val perguntasParq = listOf(
        "Algum médico já disse que você possui um problema de coração?",
        "Você sente dores no peito quando pratica atividade física?",
        "Você perde o equilíbrio devido a tontura ou já perdeu a consciência?",
    )

    // ---------- Painel do Gestor ----------
    var nomeGestor = "Prof. Carlos Andrade"
    var emailGestor = EMAIL_GESTOR

    val alunos = mutableListOf(
        Aluno("Giuseppe Chinelate", "2201234567", "giuseppe@edu.unifor.br", "Ativo", "Musculação",
            "Treino C — Pernas", "mar/2026", true),
        Aluno("Marina Costa", "2201234568", "marina@edu.unifor.br", "Ativo", "Musculação",
            "Treino A — Peito e Tríceps", "fev/2026", true),
        Aluno("Lucas Pereira", "2201234569", "lucas@edu.unifor.br", "Ativo", "Funcional",
            "Cardio/HIIT", "jan/2026", false),
        Aluno("Beatriz Lima", "2201234570", "beatriz@edu.unifor.br", "Pendente", "Musculação",
            "Treino B — Costas e Bíceps", "set/2026", true),
        Aluno("Rafael Souza", "2201234571", "rafael@edu.unifor.br", "Ativo", "Cardio",
            "Cardio/HIIT", "abr/2026", false),
    )

    val lembretes = mutableListOf(
        Lembrete("Reavaliação de anamnese", "A cada 60 dias · Todos os alunos", true),
        Lembrete("Treino não realizado", "Após 3 dias sem check-in", true),
        Lembrete("Renovação de matrícula", "7 dias antes do vencimento", true),
        Lembrete("Aniversário do aluno", "No dia do aniversário", false),
    )

    /** Avisos que o gestor escolheu receber (Perfil do Gestor → Notificações do painel). */
    val avisosPainel = linkedMapOf(
        "Anamnese pendente" to true,
        "Novo aluno cadastrado" to true,
        "Mensagem de aluno" to true,
        "Treino não realizado" to false,
    )

    val medidasFicha = listOf(
        Medida("Cintura", "82 cm", "-3cm", true),
        Medida("Quadril", "98 cm", "-1cm", true),
        Medida("Braço", "34 cm", "+1cm", true),
        Medida("Coxa", "56 cm", "+2cm", true),
        Medida("% Gordura", "18%", "-2%", true),
    )

    val historicoMedicoes = listOf(
        "10/09/2026  ·  82 · 98 · 34 · 56 · 18%",
        "10/06/2026  ·  85 · 99 · 33 · 54 · 20%",
        "10/03/2026  ·  88 · 101 · 32 · 52 · 22%",
    )

    val opcoesStatus = listOf("Ativo", "Pendente", "Inativo")
    val opcoesTurma = listOf("Musculação", "Funcional", "Cardio")
    val opcoesCategoria = listOf("Musculação", "Cardio", "Funcional")
    val opcoesNivel = listOf("Iniciante", "Intermediário", "Avançado", "Todos os níveis")
    val opcoesGrupo = listOf("Pernas", "Costas", "Peito", "Braços", "Ombros", "Cardio")
    val opcoesSetor = listOf(
        "Setor de Pernas", "Setor de Costas", "Setor de Peito", "Setor de Polias", "Setor de Cardio", "Área Livre",
    )
}
