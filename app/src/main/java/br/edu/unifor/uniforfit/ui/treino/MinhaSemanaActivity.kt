package br.edu.unifor.uniforfit.ui.treino

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.StatusDia
import br.edu.unifor.uniforfit.ui.professor.FaleProfessorActivity

/** Minha Semana (RF10) — aba "Treinos" da navbar. */
class MinhaSemanaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_minha_semana, R.id.nav_treinos)
        colorirLegenda()
    }

    /** Remontado ao voltar: concluir o treino de hoje muda o status do dia. */
    override fun onResume() {
        super.onResume()
        montarSeletor()
        montarLista()
    }

    private fun montarSeletor() {
        val seletor = findViewById<LinearLayout>(R.id.seletorDias)
        seletor.removeAllViews()
        Dados.semana.forEachIndexed { i, dia ->
            val item = layoutInflater.inflate(R.layout.item_dia_semana, seletor, false)
            val letra = item.findViewById<TextView>(R.id.tvLetra)
            val marcador = item.findViewById<View>(R.id.marcador)
            letra.text = dia.letra
            marcador.backgroundTintList = ContextCompat.getColorStateList(this, corDoMarcador(dia.status))
            marcador.visibility = if (dia.status == null) View.INVISIBLE else View.VISIBLE

            // RF10.1 — o dia atual fica destacado
            if (i == Dados.INDICE_HOJE) {
                item.setBackgroundResource(R.drawable.bg_botao_primario)
                letra.setTextColor(ContextCompat.getColor(this, R.color.branco))
                marcador.backgroundTintList = ContextCompat.getColorStateList(this, R.color.branco)
            }
            item.contentDescription = "${dia.nome}: ${dia.status?.rotulo?.lowercase() ?: "sem treino"}"
            item.setOnClickListener { abrirDia(i) }
            seletor.addView(item)
        }
    }

    private fun montarLista() {
        val lista = findViewById<LinearLayout>(R.id.listaSemana)
        lista.removeAllViews()
        Dados.semana.forEachIndexed { i, dia ->
            val status = dia.status ?: return@forEachIndexed // dia sem treino só aparece no seletor
            val card = layoutInflater.inflate(R.layout.item_treino_semana, lista, false)
            card.findViewById<TextView>(R.id.tvStatus).text =
                "${dia.nome.substringBefore("-").uppercase()} · ${status.rotulo}"
            card.findViewById<TextView>(R.id.tvNome).text = dia.titulo
            card.findViewById<TextView>(R.id.tvInfo).text = dia.descricao
            if (status == StatusDia.CONCLUIDO) {
                card.findViewById<View>(R.id.thumb).backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.verde)
            }
            if (status == StatusDia.HOJE) card.setBackgroundResource(R.drawable.bg_card_selecionado)
            // RF10.3 — o card abre o detalhe do treino
            card.setOnClickListener { abrirDia(i) }
            lista.addView(card)
        }
    }

    private fun abrirDia(indice: Int) {
        val dia = Dados.semana[indice]
        if (dia.status == StatusDia.DESCANSO) {
            Popup.mostrar(
                this,
                titulo = "Dia de descanso",
                mensagem = "${dia.nome} é dia de recuperação. Um alongamento leve de 10 a 15 minutos " +
                    "ajuda os músculos a se recuperarem para o próximo treino.",
                tipo = Popup.Tipo.INFO,
                botao = "Entendi",
                botaoSecundario = "Falar com o professor",
                aoSecundario = { abrir(FaleProfessorActivity::class.java) },
            )
            return
        }
        // Dia sem treino abre o detalhe no estado vazio (RF11.2)
        abrir(DetalheTreinoActivity::class.java) { putExtra(DetalheTreinoActivity.EXTRA_DIA, indice) }
    }

    @ColorRes
    private fun corDoMarcador(status: StatusDia?) = when (status) {
        StatusDia.CONCLUIDO -> R.color.verde
        StatusDia.HOJE, StatusDia.PLANEJADO -> R.color.azul
        else -> R.color.cinza_marcador
    }

    private fun colorirLegenda() {
        val legenda = findViewById<TextView>(R.id.tvLegenda)
        val texto = SpannableString(legenda.text)
        val cores = listOf(R.color.verde, R.color.azul, R.color.cinza_marcador)
        var inicio = 0
        cores.forEach { cor ->
            val pos = texto.indexOf('●', inicio)
            if (pos >= 0) {
                texto.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, cor)),
                    pos, pos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                inicio = pos + 1
            }
        }
        legenda.text = texto
    }
}
