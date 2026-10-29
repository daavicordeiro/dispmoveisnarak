package br.edu.unifor.uniforfit.ui.chatbot

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.dp
import br.edu.unifor.uniforfit.core.formatarDecimal
import br.edu.unifor.uniforfit.data.Dados
import java.text.Normalizer

/**
 * Assistente Unifor Fit (RF09.5.1 a RF09.5.4), aberto pelo botão flutuante ✨ de várias telas.
 * As respostas são locais (por palavra-chave) até existir um serviço de IA no back-end.
 */
class ChatbotActivity : BaseActivity() {

    private lateinit var lista: LinearLayout
    private lateinit var rolagem: ScrollView
    private lateinit var etMensagem: EditText
    private var bolhaDigitando: View? = null
    private var respostaPendente: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        lista = findViewById(R.id.listaMensagens)
        rolagem = findViewById(R.id.scrollChat)
        etMensagem = findViewById(R.id.etMensagemChat)

        // A conversa continua enquanto o app estiver aberto
        conversa.forEach { (texto, doUsuario) -> adicionarBolha(texto, doUsuario) }
        findViewById<View>(R.id.painelBoasVindas).isVisible = conversa.isEmpty()

        listOf(R.id.sugestao1, R.id.sugestao2, R.id.sugestao3).forEach { id ->
            findViewById<TextView>(id).setOnClickListener { enviar((it as TextView).text.toString()) }
        }
        findViewById<View>(R.id.btnEnviarChat).setOnClickListener { enviar(etMensagem.text.toString()) }
        etMensagem.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEND) enviar(etMensagem.text.toString())
            acao == EditorInfo.IME_ACTION_SEND
        }
    }

    override fun onDestroy() {
        respostaPendente?.let { lista.removeCallbacks(it) }
        super.onDestroy()
    }

    private fun enviar(texto: String) {
        val pergunta = texto.trim()
        if (pergunta.isEmpty() || bolhaDigitando != null) return
        // RF09.5.4 — as instruções somem e as mensagens aparecem em sequência
        findViewById<View>(R.id.painelBoasVindas).isVisible = false
        etMensagem.text.clear()
        registrar(pergunta, doUsuario = true)

        bolhaDigitando = adicionarBolha("digitando…", doUsuario = false)
        val resposta = Runnable {
            bolhaDigitando?.let { lista.removeView(it) }
            bolhaDigitando = null
            respostaPendente = null
            registrar(responder(pergunta), doUsuario = false)
        }
        respostaPendente = resposta
        lista.postDelayed(resposta, ATRASO_RESPOSTA_MS)
    }

    private fun registrar(texto: String, doUsuario: Boolean) {
        conversa += texto to doUsuario
        adicionarBolha(texto, doUsuario)
    }

    private fun adicionarBolha(texto: String, doUsuario: Boolean): View {
        val bolha = TextView(this).apply {
            text = texto
            textSize = 14f
            maxWidth = dp(280)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundResource(if (doUsuario) R.drawable.bg_bolha_usuario else R.drawable.bg_bolha_bot)
            setTextColor(ContextCompat.getColor(context, if (doUsuario) R.color.branco else R.color.texto_escuro))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
                gravity = if (doUsuario) Gravity.END else Gravity.START
            }
        }
        lista.addView(bolha)
        rolagem.post { rolagem.fullScroll(View.FOCUS_DOWN) }
        return bolha
    }

    private fun responder(pergunta: String): String {
        val texto = Normalizer.normalize(pergunta.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        val legPress = Dados.exercicio(1)
        val hoje = Dados.diaDeHoje
        return when {
            "leg" in texto -> "No ${legPress.nome} (${legPress.maquina}): ${legPress.instrucoes} " +
                "Desça até os joelhos formarem cerca de 90°."
            "carga" in texto || "peso do aparelho" in texto ->
                "Abra o treino, toque no ⓘ do exercício e depois em \"Personalizar carga\". " +
                    "Ajuste com os botões − e + ou com o controle deslizante e toque em \"Salvar carga\"."
            "ranking" in texto || "ponto" in texto ->
                "Cada treino concluído vale +${Dados.PONTOS_POR_TREINO} pontos. Você está em #${Dados.posicaoRanking} " +
                    "com ${Dados.pontos} pontos nesta semana."
            "imc" in texto ->
                "O IMC é o peso dividido pela altura ao quadrado. O seu está em ${Dados.imc().formatarDecimal()} " +
                    "(${Dados.classificacaoImc(Dados.imc()).first.lowercase()}). Recalcule em Evolução → Evolução corporal."
            "descanso" in texto || "intervalo" in texto ->
                "Ao concluir uma série o app conta o intervalo de descanso sozinho. Se já estiver pronto, toque em \"Pular descanso\"."
            "professor" in texto ->
                "Você fala com o ${Dados.nomeGestor} em Perfil → Fale com seu Professor, por mensagem ou WhatsApp."
            "treino" in texto || "hoje" in texto ->
                "Seu treino de hoje é o ${hoje.titulo}: ${hoje.descricao}. Toque em \"Treino de hoje\" no Início para começar."
            texto.startsWith("oi") || texto.startsWith("ola") ->
                "Olá, ${Dados.primeiroNome}! Como posso ajudar no seu treino hoje?"
            else -> "Ainda não sei responder isso 😅. Posso ajudar com treinos, exercícios, carga, ranking ou IMC. " +
                "Para dúvidas específicas, fale com seu professor pelo Perfil."
        }
    }

    private companion object {
        const val ATRASO_RESPOSTA_MS = 700L
        val conversa = mutableListOf<Pair<String, Boolean>>()
    }
}
