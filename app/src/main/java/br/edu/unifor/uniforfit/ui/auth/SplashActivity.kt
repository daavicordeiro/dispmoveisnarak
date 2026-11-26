package br.edu.unifor.uniforfit.ui.auth

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Rede
import br.edu.unifor.uniforfit.data.Banco
import br.edu.unifor.uniforfit.data.Semeador
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Tela de Abertura (RF01). Na primeira execução também prepara o banco ([Semeador]). */
class SplashActivity : BaseActivity() {

    override val telaEscura = true
    override val exigeSessao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val inicio = SystemClock.elapsedRealtime()
        lifecycleScope.launch {
            // Toda visita à abertura começa sem sessão: o login decide quem entra.
            Banco.sair()
            if (Rede.estaOnline(this@SplashActivity)) {
                withTimeoutOrNull(LIMITE_CARGA_MS) { Semeador.semearSeNecessario() }
            }
            // RF01.1 — fica no máximo alguns segundos e segue sozinha para o login (com fade).
            val restante = DURACAO_MS - (SystemClock.elapsedRealtime() - inicio)
            if (restante > 0) delay(restante)
            abrirComoInicio(LoginActivity::class.java)
        }
    }

    private companion object {
        const val DURACAO_MS = 1800L
        /** A carga inicial só acontece uma vez; depois a abertura dura 1,8 s. */
        const val LIMITE_CARGA_MS = 20_000L
    }
}
