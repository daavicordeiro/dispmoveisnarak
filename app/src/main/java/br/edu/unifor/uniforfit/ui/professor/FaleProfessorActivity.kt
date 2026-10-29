package br.edu.unifor.uniforfit.ui.professor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.esconderTeclado
import br.edu.unifor.uniforfit.data.Dados

/** Fale com seu Professor (RF22). Aberta pelo Perfil e pelo estado vazio "Sem treino". */
class FaleProfessorActivity : BaseActivity() {

    private lateinit var etMensagem: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uf_activity_fale_professor)

        // RF22.1
        findViewById<TextView>(R.id.tvNomeProfessor).text = Dados.nomeGestor
        findViewById<TextView>(R.id.tvCargoProfessor).text = "Educador físico responsável"

        etMensagem = findViewById(R.id.etMensagem)
        findViewById<View>(R.id.btnEnviar).setOnClickListener { enviar() }
        findViewById<View>(R.id.btnWhatsApp).setOnClickListener { confirmarWhatsApp() }
    }

    /** RF22.2 — a resposta do professor chega como notificação. */
    private fun enviar() {
        if (etMensagem.text.isBlank()) {
            etMensagem.error = getString(R.string.uf_mensagem_vazia)
            etMensagem.tremer()
            return
        }
        exigirConexao {
            etMensagem.text.clear()
            esconderTeclado()
            Popup.mostrar(
                this,
                titulo = getString(R.string.uf_mensagem_enviada),
                mensagem = "O ${Dados.nomeGestor} vai responder pelas suas notificações.",
                botao = "OK",
            )
        }
    }

    /** RF22.3 / RNF08.2 — avisa que o aluno vai sair do app antes de abrir o link externo. */
    private fun confirmarWhatsApp() {
        Popup.mostrar(
            this,
            titulo = "Abrir o WhatsApp?",
            mensagem = "Você vai sair do Unifor Fit para conversar com a academia. Nada da conversa fica salvo no app.",
            tipo = Popup.Tipo.PERGUNTA,
            botao = getString(R.string.uf_abrir_whatsapp),
            botaoSecundario = getString(R.string.uf_cancelar),
        ) {
            val link = Uri.parse("https://wa.me/$WHATSAPP_ACADEMIA?text=" + Uri.encode("Olá! Sou aluno(a) da academia Unifor."))
            try {
                startActivity(Intent(Intent.ACTION_VIEW, link))
            } catch (e: ActivityNotFoundException) {
                avisar(getString(R.string.uf_whatsapp_indisponivel))
            }
        }
    }

    private companion object {
        /** Número provisório — trocar pelo WhatsApp oficial da academia. */
        const val WHATSAPP_ACADEMIA = "5585900000000"
    }
}
