package br.edu.unifor.uniforfit.ui.auth

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.gestor.LoginGestorActivity
import br.edu.unifor.uniforfit.ui.home.HomeActivity
import br.edu.unifor.uniforfit.ui.onboarding.AnamneseActivity

/** Login (RF02) e Login — Erro de Credencial (RF02.3). */
class LoginActivity : BaseActivity() {

    private lateinit var etRa: EditText
    private lateinit var etSenha: EditText
    private lateinit var tvSubtitulo: TextView
    private lateinit var tvErro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etRa = findViewById(R.id.etRa)
        etSenha = findViewById(R.id.etSenha)
        tvSubtitulo = findViewById(R.id.tvSubtitulo)
        tvErro = findViewById(R.id.tvErroLogin)

        etRa.doAfterTextChanged { esconderErro() }
        etSenha.doAfterTextChanged { esconderErro() }
        etSenha.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) entrar()
            acao == EditorInfo.IME_ACTION_DONE
        }

        findViewById<TextView>(R.id.btnEntrar).setOnClickListener { entrar() }
        findViewById<TextView>(R.id.tvEsqueciSenha).setOnClickListener {
            abrir(RecuperarSenhaActivity::class.java) // RF02.4
        }
        findViewById<TextView>(R.id.tvPrimeiroAcesso).setOnClickListener {
            abrir(PrimeiroAcessoActivity::class.java)
        }
        findViewById<TextView>(R.id.tvAcessoGestor).setOnClickListener {
            abrir(LoginGestorActivity::class.java)
        }
    }

    private fun entrar() {
        val ra = etRa.text.toString().trim()
        val senha = etSenha.text.toString()
        if (ra.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha o R.A. e a senha.")
            return
        }
        exigirConexao {
            if (!Dados.loginAlunoValido(ra, senha)) {
                mostrarErro("R.A. ou senha incorretos. Tente novamente.")
                return@exigirConexao
            }
            Dados.entrarComo(ra)
            // Conta recém-criada que não terminou o cadastro clínico volta para a anamnese.
            if (ra in Dados.onboardingPendente) {
                abrirComoInicio(AnamneseActivity::class.java, Transicao.AVANCAR)
            } else {
                abrirComoInicio(HomeActivity::class.java)
            }
        }
    }

    private fun mostrarErro(mensagem: String) {
        tvErro.text = mensagem
        tvErro.isVisible = true
        tvSubtitulo.isVisible = false
        findViewById<TextView>(R.id.btnEntrar).tremer()
    }

    private fun esconderErro() {
        if (!tvErro.isVisible) return
        tvErro.isVisible = false
        tvSubtitulo.isVisible = true
    }
}
