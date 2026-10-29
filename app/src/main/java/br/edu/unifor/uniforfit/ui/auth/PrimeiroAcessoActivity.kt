package br.edu.unifor.uniforfit.ui.auth

import android.os.Bundle
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.onboarding.AnamneseActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Primeiro Acesso (RF03). */
class PrimeiroAcessoActivity : BaseActivity() {

    private lateinit var tilRa: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilSenha: TextInputLayout
    private lateinit var tilConfirmar: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primeiro_acesso)

        tilRa = findViewById(R.id.tilRa)
        tilEmail = findViewById(R.id.tilEmail)
        tilSenha = findViewById(R.id.tilSenha)
        tilConfirmar = findViewById(R.id.tilConfirmarSenha)

        findViewById<TextView>(R.id.btnCriarConta).setOnClickListener { criarConta() }
        // RF03.8 — o login está logo abaixo na pilha
        findViewById<TextView>(R.id.tvFazerLogin).setOnClickListener { voltar() }
    }

    private fun texto(id: Int) = findViewById<TextInputEditText>(id).text?.toString()?.trim().orEmpty()

    private fun criarConta() {
        val ra = texto(R.id.etRa)
        val email = texto(R.id.etEmail)
        val senha = texto(R.id.etSenha)
        val confirmacao = texto(R.id.etConfirmarSenha)

        tilRa.error = if (ra.length == 10) null else "Informe os 10 dígitos do R.A."
        tilEmail.error = if (email.endsWith("@edu.unifor.br", ignoreCase = true) && email.length > 14) null
        else "Use seu e-mail institucional (@edu.unifor.br)" // RF03.3
        tilSenha.error = if (senha.length >= 8) null else "A senha precisa ter no mínimo 8 caracteres" // RF03.4
        tilConfirmar.error = if (confirmacao == senha) null else "As senhas não conferem" // RF03.5

        if (listOf(tilRa, tilEmail, tilSenha, tilConfirmar).any { it.error != null }) {
            findViewById<TextView>(R.id.btnCriarConta).tremer()
            return
        }

        exigirConexao {
            if (!Dados.raMatriculado(ra)) { // RF03.6
                tilRa.error = "R.A. não encontrado entre os alunos matriculados"
                return@exigirConexao
            }
            if (ra in Dados.contas) {
                Popup.mostrar(
                    this,
                    titulo = "Você já tem conta",
                    mensagem = "Este R.A. já foi cadastrado. Entre com sua senha ou use \"Esqueci minha senha\".",
                    tipo = Popup.Tipo.INFO,
                    botao = "Ir para o login",
                    botaoSecundario = "Cancelar",
                ) { voltar() }
                return@exigirConexao
            }
            confirmarTermos(ra, senha)
        }
    }

    /** RNF04.2 — políticas de privacidade e termos de uso apresentados no primeiro acesso. */
    private fun confirmarTermos(ra: String, senha: String) {
        Popup.mostrar(
            this,
            titulo = "Termos de uso e privacidade",
            mensagem = "Seus dados de saúde (anamnese, PAR-Q e medidas) ficam visíveis apenas para você " +
                "e para o professor responsável, conforme a LGPD. Você pode pedir a exclusão deles a qualquer momento.",
            tipo = Popup.Tipo.INFO,
            botao = "Aceitar e criar conta",
            botaoSecundario = "Agora não",
        ) {
            Dados.contas[ra] = senha
            Dados.onboardingPendente += ra
            Dados.entrarComo(ra)
            // RF03.7 — conta criada segue direto para a anamnese
            abrirComoInicio(AnamneseActivity::class.java, Transicao.AVANCAR)
        }
    }
}
