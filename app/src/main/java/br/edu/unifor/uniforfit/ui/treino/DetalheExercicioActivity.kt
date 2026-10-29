package br.edu.unifor.uniforfit.ui.treino

import android.os.Bundle
import android.view.View
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.ItemTreino

/** Detalhe do Exercício (RF12). */
class DetalheExercicioActivity : BaseActivity() {

    override val telaEscura = true

    private lateinit var item: ItemTreino
    private var idTreino = 0
    private var indiceItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idTreino = intent.getIntExtra(DetalheTreinoActivity.EXTRA_TREINO, 0)
        indiceItem = intent.getIntExtra(DetalheTreinoActivity.EXTRA_ITEM, 0)
        val treino = Dados.treino(idTreino)
        if (treino == null || indiceItem !in treino.itens.indices) {
            finish()
            return
        }
        item = treino.itens[indiceItem]
        setConteudoComAssistente(R.layout.activity_detalhe_exercicio)

        findViewById<View>(R.id.btnPlay).setOnClickListener {
            avisar("O vídeo demonstrativo enviado pelo professor aparecerá aqui.")
        }
        // RF12.3
        findViewById<View>(R.id.btnPersonalizarCarga).setOnClickListener {
            abrir(PersonalizarCargaActivity::class.java) {
                putExtra(DetalheTreinoActivity.EXTRA_TREINO, idTreino)
                putExtra(DetalheTreinoActivity.EXTRA_ITEM, indiceItem)
            }
        }
    }

    /** Atualiza ao voltar de "Personalizar carga". */
    override fun onResume() {
        super.onResume()
        val exercicio = item.exercicio
        findViewById<TextView>(R.id.tvNomeExercicio).text = exercicio.nome
        findViewById<TextView>(R.id.tvMaquina).text = "${exercicio.maquina} · ${exercicio.setor}"
        findViewById<TextView>(R.id.tvSeries).text = item.series.toString()
        findViewById<TextView>(R.id.tvRepeticoes).text = item.repeticoes.toString()
        findViewById<TextView>(R.id.tvCarga).text = if (item.carga > 0) "${item.carga} kg" else "—"
        findViewById<TextView>(R.id.tvDescanso).text = "${item.descansoSeg}s"
        findViewById<TextView>(R.id.tvComoExecutar).text = exercicio.instrucoes
    }
}
