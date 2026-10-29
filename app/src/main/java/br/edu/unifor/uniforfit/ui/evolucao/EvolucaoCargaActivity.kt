package br.edu.unifor.uniforfit.ui.evolucao

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.core.dp
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.comum.GraficoBarrasView
import kotlin.math.roundToInt

/** Evolução de Carga (RF16) — aba "Evolução" da navbar. */
class EvolucaoCargaActivity : BaseActivity() {

    private var exercicio = Dados.historicoCarga.keys.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_evolucao_carga, R.id.nav_evolucao)
        exercicio = savedInstanceState?.getString(ESTADO_EXERCICIO) ?: exercicio

        // Abas carga/corporal: trocam de tela sem empilhar (as duas são a mesma aba da navbar)
        findViewById<RadioButton>(R.id.rbAbaCarga).isChecked = true
        findViewById<RadioGroup>(R.id.rgAbasEvolucao).setOnCheckedChangeListener { _, id ->
            if (id == R.id.rbAbaCorporal) {
                abrir(EvolucaoCorporalActivity::class.java, Transicao.FADE)
                finish()
            }
        }
    }

    /** Um treino concluído acrescenta sessões ao gráfico, então tudo é refeito ao voltar. */
    override fun onResume() {
        super.onResume()
        montarAbasExercicios()
        mostrarGrafico()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_EXERCICIO, exercicio)
    }

    private fun montarAbasExercicios() {
        val abas = findViewById<LinearLayout>(R.id.abasExercicios)
        abas.removeAllViews()
        Dados.historicoCarga.keys.forEach { nome ->
            val ativa = nome == exercicio
            abas.addView(TextView(this).apply {
                text = nome
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setBackgroundResource(R.drawable.bg_pill)
                backgroundTintList = ContextCompat.getColorStateList(context, if (ativa) R.color.azul else R.color.branco)
                setTextColor(ContextCompat.getColor(context, if (ativa) R.color.branco else R.color.texto_cinza))
                isSelected = ativa
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginEnd = dp(8) }
                setOnClickListener {
                    exercicio = nome
                    montarAbasExercicios()
                    mostrarGrafico()
                }
            })
        }
    }

    private fun mostrarGrafico() {
        val valores = Dados.historicoCarga[exercicio].orEmpty()
        // RF16.3 — sem registros mostra o estado vazio
        findViewById<View>(R.id.painelGrafico).isVisible = valores.isNotEmpty()
        findViewById<View>(R.id.estadoSemDados).isVisible = valores.isEmpty()
        if (valores.isEmpty()) return

        findViewById<GraficoBarrasView>(R.id.graficoCarga).valores = valores
        val inicial = valores.first()
        val evolucao = if (inicial > 0) ((valores.last() - inicial) / inicial * 100).roundToInt() else 0
        findViewById<TextView>(R.id.tvEvolucaoPct).text = (if (evolucao >= 0) "+" else "") + "$evolucao%"
        findViewById<TextView>(R.id.tvEvolucaoPeriodo).text = "Evolução em ${valores.size} semanas"
        findViewById<TextView>(R.id.tvCargaMaxima).text = "${valores.max().roundToInt()}kg"
    }

    private companion object {
        const val ESTADO_EXERCICIO = "exercicio"
    }
}
