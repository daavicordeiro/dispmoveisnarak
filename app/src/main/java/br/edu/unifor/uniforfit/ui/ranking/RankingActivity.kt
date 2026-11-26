package br.edu.unifor.uniforfit.ui.ranking

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.PosicaoRanking

/** Ranking da Semana (RF19) — aba "Ranking" da navbar. */
class RankingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_ranking, R.id.nav_ranking)
    }

    override fun onResume() {
        super.onResume()
        atualizarTela()
    }

    /** RF19.2 — a pontuação acompanha os treinos concluídos (de qualquer aluno). */
    override fun atualizarTela() {
        val ranking = Dados.rankingOrdenado()
        fun nome(p: PosicaoRanking) = if (p.ra == Dados.raAluno) "Você" else p.nome.substringBefore(" ")

        // Pódio: 1º, 2º e 3º lugar
        listOf(R.id.tvNome1 to R.id.tvPts1, R.id.tvNome2 to R.id.tvPts2, R.id.tvNome3 to R.id.tvPts3)
            .forEachIndexed { i, (idNome, idPts) ->
                val p = ranking.getOrNull(i)
                findViewById<TextView>(idNome).text = p?.let(::nome) ?: "—"
                findViewById<TextView>(idPts).text = p?.let { "${it.pontosSemana} pts" } ?: ""
            }

        // RF19.1 — demais posições, com a linha do próprio aluno destacada
        val lista = findViewById<LinearLayout>(R.id.listaRanking)
        lista.removeAllViews()
        ranking.drop(3).forEachIndexed { i, p ->
            val linha = layoutInflater.inflate(R.layout.item_ranking, lista, false)
            val eu = p.ra == Dados.raAluno
            linha.findViewById<TextView>(R.id.tvPosicao).text = "#${i + 4}"
            linha.findViewById<TextView>(R.id.tvNome).text = if (eu) "Você" else p.nome
            linha.findViewById<TextView>(R.id.tvPontos).apply {
                isVisible = eu
                text = "${p.pontosSemana} pts"
            }
            if (eu) linha.backgroundTintList = ContextCompat.getColorStateList(this, R.color.azul_claro)
            lista.addView(linha)
        }
    }
}
