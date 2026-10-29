package br.edu.unifor.uniforfit.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.ui.home.HomeActivity

/** Apresentação (RF08) — aparece uma vez, ao terminar o cadastro clínico. */
class ApresentacaoActivity : BaseActivity() {

    override val telaEscura = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apresentacao)

        // Entrada suave dos elementos, em sequência
        listOf(R.id.circuloLogo, R.id.tvTituloApresentacao, R.id.tvDescricaoApresentacao)
            .map { findViewById<View>(it) }
            .forEachIndexed { i, view ->
                view.alpha = 0f
                view.translationY = 40f
                view.animate().alpha(1f).translationY(0f).setStartDelay(120L * i).setDuration(400).start()
            }

        // RF08.1 — "Avançar" leva à tela inicial
        findViewById<TextView>(R.id.btnAvancar).setOnClickListener { abrirComoInicio(HomeActivity::class.java) }
    }
}
