package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados

/** Exercícios (RF37) — catálogo da academia. Também aberta por "Nomenclatura padrão" (RF40.4). */
class ExerciciosActivity : BaseActivity() {

    private val filtros = linkedMapOf(
        R.id.filtroTodos to "Todos",
        R.id.filtroPernas to "Pernas",
        R.id.filtroCostas to "Costas",
        R.id.filtroPeito to "Peito",
    )
    private var filtro = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercicios)
        filtro = savedInstanceState?.getString(ESTADO_FILTRO) ?: filtro

        // RF37.1 — filtros por grupo muscular
        filtros.forEach { (id, grupo) ->
            findViewById<View>(id).setOnClickListener {
                filtro = grupo
                atualizar()
            }
        }
        // RF37.4
        findViewById<View>(R.id.btnNovoExercicio).setOnClickListener { abrir(EditarExercicioActivity::class.java) }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_FILTRO, filtro)
    }

    private fun atualizar() {
        filtros.forEach { (id, grupo) ->
            val ativo = grupo == filtro
            findViewById<TextView>(id).apply {
                backgroundTintList = ContextCompat.getColorStateList(context, if (ativo) R.color.azul else R.color.azul_claro)
                setTextColor(ContextCompat.getColor(context, if (ativo) R.color.branco else R.color.texto_cinza))
                isSelected = ativo
            }
        }

        val lista = findViewById<LinearLayout>(R.id.listaExercicios)
        lista.removeAllViews()
        Dados.exercicios
            .filter { filtro == "Todos" || it.grupo == filtro }
            .forEach { exercicio ->
                val card = layoutInflater.inflate(R.layout.item_exercicio_catalogo, lista, false)
                card.findViewById<TextView>(R.id.tvNome).text = exercicio.nome
                card.findViewById<TextView>(R.id.tvMaquina).text = exercicio.maquina
                card.findViewById<TextView>(R.id.tvGrupo).text = exercicio.grupo
                // RF37.3
                card.setOnClickListener {
                    abrir(EditarExercicioActivity::class.java) { putExtra(EditarExercicioActivity.EXTRA_ID, exercicio.id) }
                }
                lista.addView(card)
            }
    }

    private companion object {
        const val ESTADO_FILTRO = "filtro"
    }
}
