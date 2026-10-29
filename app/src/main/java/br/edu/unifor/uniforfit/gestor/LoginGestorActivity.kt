package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.auth.LoginActivity

/** Login do Gestor (RF27). */
class LoginGestorActivity : BaseActivity() {

    override val telaEscura = true

    private lateinit var edtEmail: EditText
    private lateinit var edtSenha: EditText
    private lateinit var tvErro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_gestor)

        edtEmail = findViewById(R.id.edtEmail)
        edtSenha = findViewById(R.id.edtSenha)
        tvErro = findViewById(R.id.tvErroGestor)
        edtEmail.doAfterTextChanged { tvErro.isVisible = false }
        edtSenha.doAfterTextChanged { tvErro.isVisible = false }

        // RF27.3
        findViewById<TextView>(R.id.btnEntrar).setOnClickListener { entrar() }
        findViewById<TextView>(R.id.tvSouAluno).setOnClickListener { irParaLoginDoAluno() }
    }

    private fun entrar() {
        val email = edtEmail.text.toString().trim()
        val senha = edtSenha.text.toString()
        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro("Preencha o e-mail e a senha.")
            return
        }
        exigirConexao {
            // RF27.2 — credencial de aluno não entra no painel
            if (email.endsWith("@edu.unifor.br", ignoreCase = true)) {
                Popup.mostrar(
                    this,
                    titulo = "Acesso restrito à equipe",
                    mensagem = "O Painel do Gestor é exclusivo para os educadores físicos da academia. " +
                        "Alunos entram pelo app com o R.A.",
                    tipo = Popup.Tipo.PERIGO,
                    botao = "Ir para o login do aluno",
                    botaoSecundario = "Cancelar",
                ) { irParaLoginDoAluno() }
                return@exigirConexao
            }
            if (!Dados.loginGestorValido(email, senha)) {
                mostrarErro("E-mail ou senha incorretos.")
                return@exigirConexao
            }
            pedirCodigo(email)
        }
    }

    /** RNF05.3 — verificação em duas etapas para as contas de gestor. */
    private fun pedirCodigo(email: String) {
        val campos = CamposPopup(this).configurar(rotulo1 = "Código de 6 dígitos", dica1 = "000000")
        campos.campo1.inputType = InputType.TYPE_CLASS_NUMBER
        campos.campo1.filters = arrayOf(InputFilter.LengthFilter(6))
        Popup.mostrar(
            this,
            titulo = "Verificação em duas etapas",
            mensagem = "Enviamos um código para $email.\n(Demonstração: qualquer código de 6 dígitos.)",
            tipo = Popup.Tipo.INFO,
            botao = "Confirmar",
            botaoSecundario = "Cancelar",
            conteudo = campos.view,
            validar = {
                val valido = campos.campo1.text.length == 6
                if (!valido) campos.campo1.error = "Digite os 6 dígitos"
                valido
            },
        ) { abrirComoInicio(HomeGestorActivity::class.java) }
    }

    private fun mostrarErro(mensagem: String) {
        tvErro.text = mensagem
        tvErro.isVisible = true
        findViewById<TextView>(R.id.btnEntrar).tremer()
    }

    /** Volta para o login do aluno (ou o abre, se o painel for a primeira tela da pilha). */
    private fun irParaLoginDoAluno() {
        if (isTaskRoot) abrirComoInicio(LoginActivity::class.java) else voltar()
    }
}
