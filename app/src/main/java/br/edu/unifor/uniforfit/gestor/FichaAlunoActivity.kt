package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.dp
import br.edu.unifor.uniforfit.core.formatarPeso
import br.edu.unifor.uniforfit.data.Aluno
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.Notificacao
import br.edu.unifor.uniforfit.ui.comum.GraficoBarrasView
import br.edu.unifor.uniforfit.ui.comum.GraficoLinhaView

/** Ficha do Aluno (RF31) — abas Anamnese, PAR-Q, Medidas e Evolução. */
class FichaAlunoActivity : BaseActivity() {

    private lateinit var ra: String
    private lateinit var abas: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ra = intent.getStringExtra(AlunosActivity.EXTRA_RA) ?: run {
            finish()
            return
        }
        setContentView(R.layout.activity_ficha_aluno)

        // RF31.1 — troca de aba
        abas = findViewById(R.id.rgAbas)
        abas.setOnCheckedChangeListener { _, id -> mostrarAba(id) }
        mostrarAba(abas.checkedRadioButtonId)

        // RF31.5
        findViewById<View>(R.id.tvEditarDados).setOnClickListener {
            abrir(EditarAlunoActivity::class.java) { putExtra(AlunosActivity.EXTRA_RA, ra) }
        }
        // RF31.4 — "Ver histórico completo" abre a aba Evolução
        findViewById<View>(R.id.tvHistorico).setOnClickListener { abas.check(R.id.rbEvolucao) }

        findViewById<View>(R.id.btnSolicitarTeste).setOnClickListener { solicitarNovoParq() }
        findViewById<View>(R.id.btnVerAnexo).setOnClickListener { mostrarAnexo() }
        findViewById<View>(R.id.btnHistoricoMedidas).setOnClickListener { mostrarHistoricoMedidas() }

        montarMedidas()
        findViewById<GraficoBarrasView>(R.id.graficoCargaAluno).valores = Dados.historicoCarga["Leg Press"].orEmpty()
        findViewById<GraficoLinhaView>(R.id.graficoPesoAluno).valores = Dados.historicoPeso
    }

    /** Dados cadastrais podem ter mudado em "Editar Aluno"; se o aluno foi excluído, a ficha fecha. */
    override fun onResume() {
        super.onResume()
        val aluno = aluno() ?: run {
            finish()
            return
        }
        findViewById<TextView>(R.id.tvNomeAluno).text = aluno.nome
        findViewById<TextView>(R.id.tvInfoAluno).text = "R.A. ${aluno.ra} · ${aluno.status} desde ${aluno.desde}"

        findViewById<TextView>(R.id.tvPeso).text = "${Dados.pesoKg.formatarPeso()} kg"
        findViewById<TextView>(R.id.tvAltura).text = "${Dados.alturaCm} cm"
        findViewById<TextView>(R.id.tvObjetivoLesao).text =
            "Objetivo: ${Dados.objetivo} · " + Dados.lesao.ifBlank { "Sem restrições médicas" }
        findViewById<TextView>(R.id.tvCintura).text = "${Dados.cintura} cm"
        findViewById<TextView>(R.id.tvQuadril).text = "${Dados.quadril} cm"

        // RF31.7 — selo de risco
        val comRestricao = Dados.respostasParq.any { it == true }
        listOf(R.id.seloParq, R.id.seloParqResumo).forEach { id ->
            findViewById<TextView>(id).apply {
                text = if (comRestricao) "COM RESTRIÇÃO" else "SEM RISCOS"
                backgroundTintList = ContextCompat.getColorStateList(context, if (comRestricao) R.color.laranja else R.color.verde)
            }
        }
        findViewById<TextView>(R.id.tvResumoParq).text = if (comRestricao) {
            "Há respostas \"Sim\" — solicite liberação médica antes de intensificar o treino."
        } else {
            "Todas as respostas indicaram “Não” — liberado para atividade física sem restrições."
        }
        findViewById<TextView>(R.id.tvDataParq).text = "Respondido em ${Dados.parqRespondidoEm}"
        montarRespostasParq()
    }

    private fun aluno(): Aluno? = Dados.alunos.firstOrNull { it.ra == ra }

    private fun mostrarAba(id: Int) {
        findViewById<View>(R.id.painelAnamnese).isVisible = id == R.id.rbAnamnese
        findViewById<View>(R.id.painelParq).isVisible = id == R.id.rbParq
        findViewById<View>(R.id.painelMedidas).isVisible = id == R.id.rbMedidas
        findViewById<View>(R.id.painelEvolucao).isVisible = id == R.id.rbEvolucao
    }

    /** RF31.7.1 — cada pergunta com a resposta do aluno. */
    private fun montarRespostasParq() {
        val lista = findViewById<LinearLayout>(R.id.listaRespostasParq)
        lista.removeAllViews()
        Dados.perguntasParq.forEachIndexed { i, pergunta ->
            val sim = Dados.respostasParq.getOrNull(i) == true
            lista.addView(linha(pergunta, if (sim) "Sim" else "Não", if (sim) R.color.laranja else R.color.verde, emSelo = true))
        }
    }

    /** RF31.8.1 / RF31.8.2 — valor atual e variação colorida. */
    private fun montarMedidas() {
        val lista = findViewById<LinearLayout>(R.id.listaMedidas)
        lista.removeAllViews()
        Dados.medidasFicha.forEach { medida ->
            lista.addView(linha(
                "${medida.nome}   ${medida.valor}", medida.variacao,
                if (medida.melhorou) R.color.verde else R.color.laranja, emSelo = false,
            ))
        }
    }

    private fun linha(texto: String, destaque: String, cor: Int, emSelo: Boolean): View {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.fundo_cinza)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) }
        }
        linha.addView(TextView(this).apply {
            text = texto
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.texto_escuro))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        linha.addView(TextView(this).apply {
            text = destaque
            textSize = if (emSelo) 10f else 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            if (emSelo) {
                setBackgroundResource(R.drawable.bg_pill)
                backgroundTintList = ContextCompat.getColorStateList(context, cor)
                setTextColor(ContextCompat.getColor(context, R.color.branco))
                setPadding(dp(10), dp(2), dp(10), dp(2))
            } else {
                setTextColor(ContextCompat.getColor(context, cor))
            }
        })
        return linha
    }

    // ---------- Pop-ups da ficha ----------

    /** RF31.7.2 — confirma e notifica o aluno. */
    private fun solicitarNovoParq() {
        val aluno = aluno() ?: return
        Popup.mostrar(
            this,
            titulo = "Solicitar novo PAR-Q?",
            mensagem = "${aluno.nome} receberá uma notificação para responder o questionário novamente.",
            tipo = Popup.Tipo.PERGUNTA,
            botao = "Enviar solicitação",
            botaoSecundario = "Cancelar",
        ) {
            if (aluno.ra == Dados.raAluno) {
                Dados.notificacoes.add(0, Notificacao(
                    "Novo teste de PAR-Q", "Seu professor pediu que você atualize sua anamnese e o PAR-Q.", "agora", false,
                ))
            }
            Popup.mostrar(
                this,
                titulo = "Solicitação enviada",
                mensagem = "O aluno foi notificado. O novo resultado aparece aqui assim que ele responder.",
                botao = "OK",
            )
        }
    }

    /** RF31.7.3 — visualização do questionário assinado. */
    private fun mostrarAnexo() {
        val aluno = aluno() ?: return
        val respostas = Dados.perguntasParq.mapIndexed { i, pergunta ->
            "${i + 1}. $pergunta  →  ${if (Dados.respostasParq.getOrNull(i) == true) "Sim" else "Não"}"
        }.joinToString("\n")
        Popup.mostrar(
            this,
            titulo = "PAR-Q assinado",
            mensagem = "${aluno.nome} · R.A. ${aluno.ra}\nRespondido em ${Dados.parqRespondidoEm}\n\n$respostas\n\n" +
                "Assinado digitalmente pelo aluno no aplicativo.",
            tipo = Popup.Tipo.INFO,
            botao = "Fechar",
        )
    }

    /** RF31.8.3 — série histórica das medidas. */
    private fun mostrarHistoricoMedidas() {
        Popup.mostrar(
            this,
            titulo = "Histórico de medições",
            mensagem = "Data  ·  Cintura · Quadril · Braço · Coxa · % Gordura\n\n" +
                Dados.historicoMedicoes.joinToString("\n"),
            tipo = Popup.Tipo.INFO,
            botao = "Fechar",
        )
    }
}
