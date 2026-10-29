package br.edu.unifor.uniforfit.ui.perfil

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Preferencias
import br.edu.unifor.uniforfit.core.TamanhoFonte

/**
 * Acessibilidade do aluno (RF24) e do gestor (RF41) — a mesma tela, aberta pelos dois perfis.
 * O tamanho escolhido vale para o app inteiro (ver BaseActivity).
 */
class AcessibilidadeActivity : BaseActivity() {

    private val doGestor by lazy { intent.getBooleanExtra(EXTRA_GESTOR, false) }

    private val opcoes = mapOf(
        TamanhoFonte.PEQUENA to Triple(R.id.rbPequena, R.id.linhaPequena, R.id.seloPequena),
        TamanhoFonte.NORMAL to Triple(R.id.rbNormal, R.id.linhaNormal, R.id.seloNormal),
        TamanhoFonte.GRANDE to Triple(R.id.rbGrande, R.id.linhaGrande, R.id.seloGrande),
        TamanhoFonte.ENORME to Triple(R.id.rbEnorme, R.id.linhaEnorme, R.id.seloEnorme),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acessibilidade)

        // O assistente é do app do aluno; no painel do gestor o exemplo é o do RF41.2
        findViewById<TextView>(R.id.btnChatbot).isVisible = !doGestor
        val exemplo = if (doGestor) "Alunos ativos: 128 cadastrados" else "Treino de hoje: Leg Press 45°"
        listOf(R.id.textoPequena, R.id.textoNormal, R.id.textoGrande, R.id.textoEnorme)
            .forEach { findViewById<TextView>(it).text = exemplo }

        val atual = Preferencias.tamanhoFonte(this)
        findViewById<RadioButton>(opcoes.getValue(atual).first).isChecked = true
        destacarExemplo(atual)

        findViewById<RadioGroup>(R.id.rgFonte).setOnCheckedChangeListener { _, id ->
            val escolhido = opcoes.entries.first { it.value.first == id }.key
            if (escolhido != Preferencias.tamanhoFonte(this)) {
                Preferencias.salvarTamanhoFonte(this, escolhido)
                recreate() // redesenha já com a nova escala
            }
        }
    }

    /** RF24.1 / RF41.2 — a opção selecionada fica destacada também no "Veja como fica". */
    private fun destacarExemplo(selecionado: TamanhoFonte) {
        opcoes.forEach { (tamanho, ids) ->
            val ativo = tamanho == selecionado
            findViewById<View>(ids.second).backgroundTintList =
                ContextCompat.getColorStateList(this, if (ativo) R.color.azul_claro else R.color.fundo_cinza)
            findViewById<TextView>(ids.third).apply {
                backgroundTintList = ContextCompat.getColorStateList(context, if (ativo) R.color.azul else R.color.branco)
                setTextColor(ContextCompat.getColor(context, if (ativo) R.color.branco else R.color.texto_cinza))
            }
        }
    }

    companion object {
        const val EXTRA_GESTOR = "gestor"
    }
}
