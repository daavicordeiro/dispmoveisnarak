package br.edu.unifor.uniforfit.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.StatusDia
import br.edu.unifor.uniforfit.ui.notificacoes.NotificacoesActivity
import br.edu.unifor.uniforfit.ui.ranking.RankingActivity
import br.edu.unifor.uniforfit.ui.treino.DetalheTreinoActivity
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.core.formatarPeso
import java.util.Calendar

/** Início (RF09) — base da pilha do aluno; as outras abas da navbar abrem por cima dela. */
class HomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_home, R.id.nav_inicio)

        findViewById<View>(R.id.btnNotificacoes).setOnClickListener {
            abrir(NotificacoesActivity::class.java)
        }
        // RF09.3 — card do treino de hoje abre o detalhe do treino (RF11)
        findViewById<View>(R.id.cardTreinoHoje).setOnClickListener { abrirTreinoDeHoje() }
        // "Seu ranking" leva para a aba Ranking
        findViewById<View>(R.id.cardRanking).setOnClickListener {
            abrir(RankingActivity::class.java, Transicao.FADE)
        }
    }

    /** Os números mudam depois de um treino concluído, por isso são atualizados ao voltar para cá. */
    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    override fun atualizarTela() {
        findViewById<TextView>(R.id.tvSaudacao).text = "${saudacao()},"
        findViewById<TextView>(R.id.tvNomeAluno).text = Dados.primeiroNome
        findViewById<TextView>(R.id.tvTreinosMes).text = Dados.treinosMes.toString()
        findViewById<TextView>(R.id.tvCargaEvoluida).text =
            "+" + Dados.cargaEvoluidaKg.formatarPeso() + "kg"
        val posicao = Dados.posicaoRanking
        findViewById<TextView>(R.id.tvPosicaoRanking).text = "#$posicao"
        // RF09.4 — variação em relação à posição da semana anterior
        val anterior = Dados.aluno.posicaoSemanaPassada
        val variacao = if (anterior > 0 && posicao > 0) anterior - posicao else 0
        findViewById<TextView>(R.id.tvVariacaoRanking).text = when {
            variacao > 1 -> "Você subiu $variacao posições"
            variacao == 1 -> "Você subiu 1 posição"
            variacao == -1 -> "Você caiu 1 posição"
            variacao < -1 -> "Você caiu ${-variacao} posições"
            else -> "Você manteve sua posição"
        }
        findViewById<View>(R.id.badgeNotificacoes).isVisible = Dados.notificacoes.any { !it.lida }

        val hoje = Dados.diaDeHoje
        findViewById<TextView>(R.id.tvTreinoHojeNome).text = hoje.titulo
        findViewById<TextView>(R.id.tvTreinoHojeInfo).text =
            if (hoje.status == StatusDia.CONCLUIDO) "Concluído hoje ✓" else hoje.descricao
        findViewById<View>(R.id.thumbTreinoHoje).backgroundTintList =
            if (hoje.status == StatusDia.CONCLUIDO) ContextCompat.getColorStateList(this, R.color.verde) else null
    }

    private fun abrirTreinoDeHoje() {
        val hoje = Dados.diaDeHoje
        if (hoje.status == StatusDia.CONCLUIDO) {
            Popup.mostrar(
                this,
                titulo = "Treino de hoje concluído",
                mensagem = "Você já fez o ${hoje.titulo} hoje. Quer revisar os exercícios mesmo assim?",
                tipo = Popup.Tipo.SUCESSO,
                botao = "Ver treino",
                botaoSecundario = "Fechar",
            ) { abrir(DetalheTreinoActivity::class.java) { putExtra(DetalheTreinoActivity.EXTRA_DIA, Dados.INDICE_HOJE) } }
            return
        }
        abrir(DetalheTreinoActivity::class.java) { putExtra(DetalheTreinoActivity.EXTRA_DIA, Dados.INDICE_HOJE) }
    }

    private fun saudacao() = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}
