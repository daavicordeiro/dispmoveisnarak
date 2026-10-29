package br.edu.unifor.uniforfit.ui.notificacoes

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.data.Notificacao
import com.google.android.material.card.MaterialCardView

/** Lista de notificações. Fundo azul-claro = não lida; cinza-claro = lida (RF20.2). */
class NotificacaoAdapter(
    private val itens: List<Notificacao>,
    private val aoAbrir: (posicao: Int) -> Unit,
) : RecyclerView.Adapter<NotificacaoAdapter.Item>() {

    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardNotificacao)
        val titulo: TextView = view.findViewById(R.id.tvTitulo)
        val mensagem: TextView = view.findViewById(R.id.tvMensagem)
        val tempo: TextView = view.findViewById(R.id.tvTempo)
    }

    override fun onCreateViewHolder(pai: ViewGroup, tipo: Int) =
        Item(LayoutInflater.from(pai.context).inflate(R.layout.uf_item_notificacao, pai, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(item: Item, posicao: Int) {
        val notificacao = itens[posicao]
        val contexto = item.itemView.context
        item.titulo.text = notificacao.titulo
        item.mensagem.text = notificacao.mensagem
        item.tempo.text = notificacao.tempo
        item.titulo.setTypeface(null, if (notificacao.lida) Typeface.NORMAL else Typeface.BOLD)
        item.card.setCardBackgroundColor(
            ContextCompat.getColor(contexto, if (notificacao.lida) R.color.uf_fundo_suave else R.color.uf_azul_claro)
        )
        item.card.contentDescription =
            (if (notificacao.lida) "" else "Não lida. ") + "${notificacao.titulo}. ${notificacao.mensagem}. ${notificacao.tempo}"
        item.card.setOnClickListener { aoAbrir(item.bindingAdapterPosition) }
    }
}
