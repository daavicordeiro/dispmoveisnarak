package br.edu.unifor.uniforfit.ui.auth

import android.os.Bundle
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity

/** Tela de Abertura (RF01). */
class SplashActivity : BaseActivity() {

    override val telaEscura = true

    private val irParaLogin = Runnable { abrirComoInicio(LoginActivity::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        // RF01.1 — no máximo 3 segundos e segue sozinha para o login (com fade).
        window.decorView.postDelayed(irParaLogin, DURACAO_MS)
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(irParaLogin)
        super.onDestroy()
    }

    private companion object {
        const val DURACAO_MS = 1800L
    }
}
