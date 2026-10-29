package br.edu.unifor.uniforfit.ui.treino

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.StatusDia
import br.edu.unifor.uniforfit.data.Treino
import br.edu.unifor.uniforfit.ui.professor.FaleProfessorActivity

/** Detalhe do Treino (RF11) e Estado Vazio — Sem Treino (RF11.2). */
class DetalheTreinoActivity : BaseActivity() {

    private val indiceDia by lazy { intent.getIntExtra(EXTRA_DIA, Dados.INDICE_HOJE) }
    private var selecionado = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComAssistente(R.layout.activity_detalhe_treino)
        selecionado = savedInstanceState?.getInt(ESTADO_SELECIONADO) ?: 0

        val dia = Dados.semana[indiceDia]
        val treino = dia.treino
        findViewById<TextView>(R.id.tvVoltar).text = if (treino == null) "← Minha semana" else "← ${dia.nome}"

        if (treino == null) {
            findViewById<View>(R.id.conteudoTreino).isVisible = false
            findViewById<View>(R.id.estadoVazio).isVisible = true
            // RF11.3
            findViewById<View>(R.id.btnFalarProfessor).setOnClickListener {
                abrir(FaleProfessorActivity::class.java)
            }
            return
        }

        findViewById<TextView>(R.id.tvNomeTreino).text = treino.nome
        findViewById<TextView>(R.id.tvResumoTreino).text = "${treino.resumo} · Nível ${treino.nivel.lowercase()}"
        findViewById<View>(R.id.btnIniciarTreino).setOnClickListener { iniciar(treino, dia.status) }
    }

    /** A carga pode ter mudado em "Personalizar carga", então a lista é refeita ao voltar. */
    override fun onResume() {
        super.onResume()
        Dados.semana[indiceDia].treino?.let { montarLista(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ESTADO_SELECIONADO, selecionado)
    }

    private fun montarLista(treino: Treino) {
        val lista = findViewById<LinearLayout>(R.id.listaExercicios)
        lista.removeAllViews()
        selecionado = selecionado.coerceIn(0, (treino.itens.size - 1).coerceAtLeast(0))
        treino.itens.forEachIndexed { i, item ->
            val card = layoutInflater.inflate(R.layout.item_exercicio_treino, lista, false)
            card.findViewById<TextView>(R.id.tvNome).text = item.exercicio.nome
            card.findViewById<TextView>(R.id.tvMaquina).text = item.exercicio.maquina
            card.findViewById<TextView>(R.id.tvPrescricao).text = item.prescricao
            // RF11.6 — o exercício tocado fica marcado e o treino começa por ele
            card.setBackgroundResource(if (i == selecionado) R.drawable.bg_card_selecionado else R.drawable.bg_card_clicavel)
            card.isSelected = i == selecionado
            card.setOnClickListener {
                selecionado = i
                montarLista(treino)
            }
            // RF11.7 — botão de detalhes leva ao Detalhe do Exercício
            card.findViewById<View>(R.id.btnDetalhes).setOnClickListener {
                abrir(DetalheExercicioActivity::class.java) {
                    putExtra(EXTRA_TREINO, treino.id)
                    putExtra(EXTRA_ITEM, i)
                }
            }
            lista.addView(card)
        }
        findViewById<TextView>(R.id.btnIniciarTreino).text =
            if (selecionado == 0) "Iniciar treino" else "Iniciar por ${treino.itens[selecionado].exercicio.nome}"
    }

    /** RF11.8 — inicia a execução guiada a partir do exercício selecionado. */
    private fun iniciar(treino: Treino, status: StatusDia?) {
        if (treino.itens.isEmpty()) {
            Popup.mostrar(
                this,
                titulo = "Treino sem exercícios",
                mensagem = "Seu professor ainda não adicionou exercícios a este treino.",
                tipo = Popup.Tipo.INFO,
                botao = "Falar com o professor",
                botaoSecundario = "Fechar",
            ) { abrir(FaleProfessorActivity::class.java) }
            return
        }
        val comecar = {
            abrir(ExecucaoSerieActivity::class.java, Transicao.SUBIR) {
                putExtra(EXTRA_TREINO, treino.id)
                putExtra(EXTRA_ITEM, selecionado)
            }
        }
        val aviso = when (status) {
            StatusDia.PLANEJADO -> "Este treino está planejado para ${Dados.semana[indiceDia].nome.lowercase()}. Quer adiantá-lo para hoje?"
            StatusDia.CONCLUIDO -> "Você já concluiu este treino nesta semana. Quer fazê-lo de novo?"
            else -> null
        }
        if (aviso == null) {
            comecar()
            return
        }
        Popup.mostrar(
            this,
            titulo = "Iniciar este treino?",
            mensagem = aviso,
            tipo = Popup.Tipo.PERGUNTA,
            botao = "Iniciar treino",
            botaoSecundario = "Cancelar",
        ) { comecar() }
    }

    companion object {
        const val EXTRA_DIA = "dia"
        const val EXTRA_TREINO = "treino"
        const val EXTRA_ITEM = "item"
        private const val ESTADO_SELECIONADO = "selecionado"
    }
}
