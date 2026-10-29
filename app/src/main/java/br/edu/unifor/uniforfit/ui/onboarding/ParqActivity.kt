package br.edu.unifor.uniforfit.ui.onboarding

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Teste de PAR-Q — Etapa 2 de 3 (RF06). */
class ParqActivity : BaseActivity() {

    private val modoEdicao by lazy { intent.getBooleanExtra(AnamneseActivity.EXTRA_MODO_EDICAO, false) }

    private lateinit var grupos: List<RadioGroup>
    private lateinit var btnContinuar: TextView
    private val opcoesSim = listOf(R.id.rbSim1, R.id.rbSim2, R.id.rbSim3)
    private val opcoesNao = listOf(R.id.rbNao1, R.id.rbNao2, R.id.rbNao3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parq)

        grupos = listOf(R.id.rgPergunta1, R.id.rgPergunta2, R.id.rgPergunta3).map { findViewById(it) }
        btnContinuar = findViewById(R.id.btnContinuar)

        if (savedInstanceState == null) {
            grupos.forEachIndexed { i, grupo ->
                // No primeiro acesso nenhuma resposta vem marcada; na edição, as respostas salvas.
                val resposta = Dados.respostasParq[i]
                if (!modoEdicao || resposta == null) grupo.clearCheck()
                else grupo.check(if (resposta) opcoesSim[i] else opcoesNao[i])
            }
        }
        grupos.forEach { it.setOnCheckedChangeListener { _, _ -> atualizarBotao() } }
        atualizarBotao()

        btnContinuar.setOnClickListener { continuar() }
    }

    /** RF06.3 — "Continuar" só fica disponível com as três perguntas respondidas. */
    private fun atualizarBotao() {
        val todasRespondidas = grupos.all { it.checkedRadioButtonId != -1 }
        btnContinuar.isEnabled = todasRespondidas
        btnContinuar.alpha = if (todasRespondidas) 1f else 0.4f
    }

    private fun continuar() {
        val respostas = grupos.mapIndexed { i, grupo -> grupo.checkedRadioButtonId == opcoesSim[i] }
        respostas.forEachIndexed { i, sim -> Dados.respostasParq[i] = sim }
        // RF06.2 — registra a data de preenchimento
        Dados.parqRespondidoEm = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        if (respostas.any { it }) {
            // RF06.2 — classificado "com restrição": avisa antes de seguir
            Popup.mostrar(
                this,
                titulo = "Atenção à sua saúde",
                mensagem = "Você respondeu \"Sim\" a pelo menos uma pergunta. Recomendamos uma liberação médica " +
                    "antes de treinar. Seu professor será avisado e vai adaptar o seu treino.",
                tipo = Popup.Tipo.ALERTA,
                botao = "Entendi, continuar",
                botaoSecundario = "Revisar respostas",
            ) { irParaMedidas() }
        } else {
            irParaMedidas()
        }
    }

    private fun irParaMedidas() =
        abrir(MedidasActivity::class.java) { putExtra(AnamneseActivity.EXTRA_MODO_EDICAO, modoEdicao) }
}
