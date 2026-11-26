package br.edu.unifor.uniforfit.ui.auth

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Banco
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Recuperar Senha (RF04). O "← Voltar para o login" é ligado pela BaseActivity. */
class RecuperarSenhaActivity : BaseActivity() {

    override val exigeSessao = false

    private lateinit var etRa: EditText
    private lateinit var tvErro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_senha)

        etRa = findViewById(R.id.etRa)
        tvErro = findViewById(R.id.tvErro)
        etRa.doAfterTextChanged { tvErro.isVisible = false }
        etRa.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) enviarLink()
            acao == EditorInfo.IME_ACTION_DONE
        }
        findViewById<TextView>(R.id.btnEnviarLink).setOnClickListener { enviarLink() }
    }

    private fun enviarLink() {
        val ra = etRa.text.toString().trim()
        if (ra.length != 10) {
            mostrarErro("Informe os 10 dígitos do R.A.")
            return
        }
        exigirConexao {
            val botao = findViewById<TextView>(R.id.btnEnviarLink)
            botao.isEnabled = false
            lifecycleScope.launch {
                // RF04.2 — só dispara o e-mail se o R.A. existir na base
                val (resultado, email) = Banco.recuperarSenha(ra)
                botao.isEnabled = true
                when {
                    resultado == Banco.Resultado.Ok && email != null -> Popup.mostrar(
                        this@RecuperarSenhaActivity,
                        titulo = "Link enviado!",
                        mensagem = "Enviamos um link de recuperação para ${mascarar(email)}. Confira sua caixa de entrada.",
                        botao = "Voltar para o login",
                        cancelavel = false,
                    ) { voltar() }
                    resultado == Banco.Resultado.NaoEncontrado ->
                        mostrarErro("R.A. não encontrado. Confira os números e tente novamente.")
                    else -> avisar("Não foi possível enviar o link agora. Tente novamente.")
                }
            }
        }
    }

    private fun mostrarErro(mensagem: String) {
        tvErro.text = mensagem
        tvErro.isVisible = true
        etRa.tremer()
    }

    /** "giuseppe@edu.unifor.br" → "g•••••••@edu.unifor.br" */
    private fun mascarar(email: String): String {
        val usuario = email.substringBefore("@")
        return usuario.take(1) + "•".repeat((usuario.length - 1).coerceAtLeast(3)) + "@" + email.substringAfter("@")
    }
}
