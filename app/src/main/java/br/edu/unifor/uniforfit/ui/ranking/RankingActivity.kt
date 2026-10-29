package br.edu.unifor.uniforfit.ui.ranking

import android.os.Bundle
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados

/** Ranking da Semana (RF19) — aba "Ranking" da navbar. */
class RankingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_ranking, R.id.nav_ranking)
    }

    override fun onResume() {
        super.onResume()
        // RF19.2 — a pontuação acompanha os treinos concluídos
        findViewById<TextView>(R.id.tvPontosVoce).text = "${Dados.pontos} pts"
    }
}
