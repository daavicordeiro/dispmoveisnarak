package br.edu.unifor.uniforfit.ui.notificacoes

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.Notificacao
import br.edu.unifor.uniforfit.ui.evolucao.EvolucaoCargaActivity
import br.edu.unifor.uniforfit.ui.onboarding.AnamneseActivity
import br.edu.unifor.uniforfit.ui.ranking.RankingActivity
import br.edu.unifor.uniforfit.ui.treino.DetalheTreinoActivity

/** Notificações (RF20) e Notificações — Estado Vazio (RF20.4). Aberta pelo sino do Início. */
class NotificacoesActivity : BaseActivity() {

    private lateinit var adaptador: NotificacaoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uf_activity_notificacoes)

        adaptador = NotificacaoAdapter(Dados.notificacoes) { posicao -> abrirNotificacao(posicao) }
        findViewById<RecyclerView>(R.id.rvNotificacoes).apply {
            layoutManager = LinearLayoutManager(this@NotificacoesActivity)
            adapter = adaptador
        }
        findViewById<View>(R.id.btnLimpar).setOnClickListener { confirmarLimpeza() }
        atualizarEstado()
    }

    private fun atualizarEstado() {
        val vazia = Dados.notificacoes.isEmpty()
        findViewById<View>(R.id.tvVazio).isVisible = vazia
        findViewById<View>(R.id.rvNotificacoes).isVisible = !vazia
        findViewById<View>(R.id.btnLimpar).isVisible = !vazia
    }

    /** Pop-up com o aviso completo; abrir marca como lida (RF20.2) e oferece o atalho da tela relacionada. */
    private fun abrirNotificacao(posicao: Int) {
        val notificacao = Dados.notificacoes.getOrNull(posicao) ?: return
        notificacao.lida = true
        adaptador.notifyItemChanged(posicao)

        val atalho = atalhoPara(notificacao)
        Popup.mostrar(
            this,
            titulo = notificacao.titulo,
            mensagem = "${notificacao.mensagem}\n\n${notificacao.tempo}",
            tipo = Popup.Tipo.INFO,
            botao = atalho?.first ?: "Fechar",
            botaoSecundario = if (atalho != null) "Fechar" else null,
        ) { atalho?.second?.invoke() }
    }

    private fun atalhoPara(notificacao: Notificacao): Pair<String, () -> Unit>? {
        val texto = (notificacao.titulo + " " + notificacao.mensagem).lowercase()
        return when {
            "recorde" in texto -> "Ver evolução" to { abrir(EvolucaoCargaActivity::class.java, Transicao.FADE) }
            "ranking" in texto -> "Ver ranking" to { abrir(RankingActivity::class.java, Transicao.FADE) }
            "anamnese" in texto -> "Atualizar anamnese" to {
                abrir(AnamneseActivity::class.java) { putExtra(AnamneseActivity.EXTRA_MODO_EDICAO, true) }
            }
            "treino" in texto && !notificacao.titulo.startsWith("Treino concluído") -> "Ver treino de hoje" to {
                abrir(DetalheTreinoActivity::class.java) { putExtra(DetalheTreinoActivity.EXTRA_DIA, Dados.INDICE_HOJE) }
            }
            else -> null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun confirmarLimpeza() {
        Popup.mostrar(
            this,
            titulo = "Limpar notificações?",
            mensagem = "Todos os avisos serão removidos desta lista.",
            tipo = Popup.Tipo.PERGUNTA,
            botao = "Limpar tudo",
            botaoSecundario = "Cancelar",
        ) {
            Dados.notificacoes.clear()
            adaptador.notifyDataSetChanged()
            atualizarEstado()
        }
    }
}
