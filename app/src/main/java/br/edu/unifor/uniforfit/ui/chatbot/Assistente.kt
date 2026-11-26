package br.edu.unifor.uniforfit.ui.chatbot

import android.util.Log
import br.edu.unifor.uniforfit.core.formatarDecimal
import br.edu.unifor.uniforfit.data.Dados
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.content

/**
 * Assistente Unifor Fit com Gemini (Firebase AI Logic). A instrução de sistema limita as
 * respostas ao assunto academia e traz os dados do aluno logado para respostas personalizadas.
 */
object Assistente {

    private const val TAG = "Assistente"
    private const val MODELO = "gemini-3.5-flash-lite"

    private var chat: Chat? = null
    private var chatDoRa = ""

    /** Resposta do Gemini ou `null` se o serviço não estiver disponível (sem internet, App Check...). */
    suspend fun perguntar(pergunta: String): String? = try {
        conversa().sendMessage(pergunta).text?.trim()?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        Log.w(TAG, "Gemini indisponível; usando as respostas locais", e)
        null
    }

    /** Começa uma conversa nova (ao trocar de aluno ou sair da conta). */
    fun reiniciar() {
        chat = null
        chatDoRa = ""
    }

    private fun conversa(): Chat {
        val atual = chat
        if (atual != null && chatDoRa == Dados.raAluno) return atual
        val modelo = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODELO,
            systemInstruction = content { text(instrucoes()) },
            requestOptions = RequestOptions(timeoutInMillis = 30_000),
        )
        return modelo.startChat().also {
            chat = it
            chatDoRa = Dados.raAluno
        }
    }

    private fun instrucoes(): String {
        val hoje = Dados.diaDeHoje
        val treinoHoje = hoje.treino?.let { t ->
            "${t.nome}: " + t.itens.joinToString("; ") { "${it.exercicio.nome} (${it.exercicio.maquina}) ${it.prescricao}" }
        } ?: hoje.titulo
        val catalogo = Dados.exercicios.joinToString("\n") { "- ${it.nome} — ${it.maquina}: ${it.instrucoes}" }
        val semana = Dados.semana.joinToString("\n") { dia ->
            val t = dia.treino
            "- ${dia.nome}: " + if (t == null) dia.titulo else
                "${t.nome} (${dia.status?.rotulo?.lowercase()}) — " +
                    t.itens.joinToString("; ") { "${it.exercicio.nome} ${it.prescricao}" }
        }
        return """
            Você é o "Assistente Unifor Fit", assistente virtual do aplicativo da academia da Universidade de Fortaleza (Unifor).
            Responda sempre em português do Brasil, de forma curta (no máximo 4 frases), simpática e direta.
            Fale APENAS sobre academia: treinos, exercícios, execução correta, carga, descanso, evolução, IMC, ranking e
            o uso do aplicativo Unifor Fit. Se a pergunta for sobre qualquer outro assunto, recuse com educação e diga
            que só pode ajudar com treinos e com o app. Não dê diagnósticos médicos: em caso de dor ou problema de saúde,
            oriente a procurar o professor ou um médico.

            Como o app funciona:
            - Cada treino concluído vale +${Dados.PONTOS_POR_TREINO} pontos no ranking da semana.
            - Para mudar a carga: abrir o treino, tocar no ícone "i" do exercício e em "Personalizar carga".
            - Ao concluir uma série o app conta o descanso sozinho; dá para "Pular descanso".
            - O IMC é recalculado em Evolução → Evolução corporal → "Registrar novo peso/IMC".
            - Para falar com o professor: Perfil → Fale com seu Professor (mensagem ou WhatsApp).

            Dados do aluno:
            - Nome: ${Dados.nomeAluno}; objetivo: ${Dados.objetivo.ifBlank { "não informado" }};
              lesão/restrição: ${Dados.lesao.ifBlank { "nenhuma" }}.
            - Peso ${Dados.pesoKg.formatarDecimal()} kg, altura ${Dados.alturaCm} cm, IMC ${Dados.imc().formatarDecimal()}.
            - Treino de hoje (${hoje.nome}): $treinoHoje.
            - Semana de treinos montada pelo professor:
            $semana
            - Ranking: posição #${Dados.posicaoRanking}, ${Dados.pontos} pontos no total.
            - Professor responsável: ${Dados.professorDoAluno}.

            Exercícios da academia (nome — máquina: como executar):
            $catalogo
        """.trimIndent()
    }
}
