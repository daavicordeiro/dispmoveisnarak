package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados

/** Treinos (RF32 / RF35) — modelos de treino da academia. */
class TreinosListaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treinos_lista)
        // RF35.3
        findViewById<View>(R.id.btnAdicionarTreino).setOnClickListener { abrir(EditarTreinoActivity::class.java) }
    }

    override fun onResume() {
        super.onResume()
        val lista = findViewById<LinearLayout>(R.id.listaTreinos)
        lista.removeAllViews()
        Dados.treinos.forEach { treino ->
            val card = layoutInflater.inflate(R.layout.item_treino_gestor, lista, false)
            card.findViewById<TextView>(R.id.tvNome).text = treino.nome
            card.findViewById<TextView>(R.id.tvCategoria).text = "${treino.categoria} · ${treino.nivel}"
            card.findViewById<TextView>(R.id.tvAlunos).text = "${treino.alunos} alunos"
            // RF35.2
            card.setOnClickListener {
                abrir(EditarTreinoActivity::class.java) { putExtra(EditarTreinoActivity.EXTRA_ID, treino.id) }
            }
            lista.addView(card)
        }
    }
}
