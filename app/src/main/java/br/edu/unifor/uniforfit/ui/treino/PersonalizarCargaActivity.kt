package br.edu.unifor.uniforfit.ui.treino

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.ItemTreino
import br.edu.unifor.uniforfit.data.Notificacao

/** Personalizar Carga (RF13). */
class PersonalizarCargaActivity : BaseActivity() {

    private lateinit var item: ItemTreino
    private lateinit var txtCarga: TextView
    private lateinit var txtUltima: TextView
    private lateinit var seek: SeekBar
    private var carga = 0
    private var ultimaRegistrada = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val treino = Dados.treino(intent.getIntExtra(DetalheTreinoActivity.EXTRA_TREINO, 0))
        val indice = intent.getIntExtra(DetalheTreinoActivity.EXTRA_ITEM, 0)
        if (treino == null || indice !in treino.itens.indices) {
            finish()
            return
        }
        item = treino.itens[indice]
        setContentView(R.layout.activity_personalizar_carga)

        txtCarga = findViewById(R.id.txtCarga)
        txtUltima = findViewById(R.id.txtUltimaCarga)
        seek = findViewById(R.id.seekCarga)
        findViewById<TextView>(R.id.txtExercicio).text = "${item.exercicio.nome} · ajuste conforme sua evolução"

        ultimaRegistrada = Dados.ultimaCargaRegistrada[item.exercicio.id] ?: item.carga
        definirCarga(savedInstanceState?.getInt(ESTADO_CARGA) ?: item.carga)

        // RF13.1 — botões "−" / "+" e controle deslizante
        findViewById<TextView>(R.id.btnDiminuir).setOnClickListener { definirCarga(carga - PASSO_KG) }
        findViewById<TextView>(R.id.btnAumentar).setOnClickListener { definirCarga(carga + PASSO_KG) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(barra: SeekBar, progresso: Int, doUsuario: Boolean) {
                if (doUsuario) definirCarga(progresso)
            }

            override fun onStartTrackingTouch(barra: SeekBar) = Unit
            override fun onStopTrackingTouch(barra: SeekBar) = Unit
        })
        findViewById<TextView>(R.id.btnSalvarCarga).setOnClickListener { salvar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ESTADO_CARGA, carga)
    }

    private fun definirCarga(valor: Int) {
        carga = valor.coerceIn(0, seek.max)
        txtCarga.text = "$carga kg"
        txtCarga.contentDescription = "Carga atual: $carga quilos"
        if (seek.progress != carga) seek.progress = carga

        // RF13.2 — última carga registrada e variação em relação ao valor atual
        val diferenca = carga - ultimaRegistrada
        val variacao = when {
            diferenca > 0 -> " (+${diferenca}kg hoje)"
            diferenca < 0 -> " (${diferenca}kg hoje)"
            else -> ""
        }
        txtUltima.text = "Última carga registrada: $ultimaRegistrada kg$variacao"
        txtUltima.setTextColor(
            ContextCompat.getColor(this, if (diferenca >= 0) R.color.verde_sucesso else R.color.laranja)
        )
    }

    /** RF13.3 — grava a carga no histórico e atualiza a prescrição do exercício. */
    private fun salvar() {
        if (carga == item.carga) {
            avisar("A carga continua em $carga kg.")
            return
        }
        val chave = Dados.chaveHistorico(item.exercicio)
        val recordeAnterior = chave?.let { Dados.historicoCarga[it]?.maxOrNull()?.toInt() } ?: item.carga
        val bateuRecorde = carga > recordeAnterior

        Dados.ultimaCargaRegistrada[item.exercicio.id] = item.carga
        item.carga = carga

        if (bateuRecorde) {
            Dados.notificacoes.add(0, Notificacao(
                "Novo recorde 🎉", "Você bateu recorde no ${item.exercicio.nome}: ${carga}kg.", "agora", false,
            ))
        }
        Popup.mostrar(
            this,
            titulo = if (bateuRecorde) "Novo recorde! 🎉" else "Carga salva!",
            mensagem = if (bateuRecorde) {
                "$carga kg é a maior carga que você já usou no ${item.exercicio.nome}. Ela vale a partir da próxima série."
            } else {
                "A prescrição do ${item.exercicio.nome} agora é de $carga kg."
            },
            botao = "Voltar ao exercício",
            cancelavel = false,
        ) { finish() }
    }

    private companion object {
        const val PASSO_KG = 5
        const val ESTADO_CARGA = "carga"
    }
}
