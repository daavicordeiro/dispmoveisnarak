package br.edu.unifor.uniforfit.core

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Pop-up padrão do Unifor Fit, com o visual do "Popup — Treino Concluído" (RF15):
 * ícone colorido, título, mensagem, conteúdo extra opcional e até dois botões.
 */
class Popup private constructor(context: Context) : Dialog(context, R.style.UniforFitPopup) {

    enum class Tipo(val simbolo: String, @ColorRes val cor: Int) {
        SUCESSO("✓", R.color.verde),
        INFO("i", R.color.azul),
        PERGUNTA("?", R.color.azul),
        TEMPO("⏱", R.color.azul),
        ALERTA("!", R.color.laranja),
        PERIGO("!", R.color.vermelho),
    }

    private val tvIcone: TextView
    private val tvTitulo: TextView
    private val tvMensagem: TextView
    private val moldura: FrameLayout
    private val btnPrimario: Button
    private val btnSecundario: TextView

    init {
        setContentView(R.layout.popup_padrao)
        tvIcone = findViewById(R.id.popupIcone)
        tvTitulo = findViewById(R.id.popupTitulo)
        tvMensagem = findViewById(R.id.popupMensagem)
        moldura = findViewById(R.id.popupConteudo)
        btnPrimario = findViewById(R.id.popupPrimario)
        btnSecundario = findViewById(R.id.popupSecundario)
    }

    /** Usado por pop-ups "vivos", como a contagem do intervalo de descanso. */
    fun atualizar(titulo: CharSequence? = null, mensagem: CharSequence? = null) {
        titulo?.let { tvTitulo.text = it }
        mensagem?.let { tvMensagem.text = it }
    }

    override fun show() {
        super.show()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    companion object {

        /**
         * Mostra um pop-up. [validar] permite manter o pop-up aberto (ex.: formulário incompleto);
         * [aoConfirmar] roda depois que ele fecha.
         */
        fun mostrar(
            context: Context,
            titulo: String,
            mensagem: CharSequence? = null,
            tipo: Tipo? = Tipo.SUCESSO,
            botao: String = "OK",
            botaoSecundario: String? = null,
            conteudo: View? = null,
            cancelavel: Boolean = true,
            validar: () -> Boolean = { true },
            aoSecundario: () -> Unit = {},
            aoConfirmar: () -> Unit = {},
        ): Popup {
            val popup = Popup(context)
            popup.setCancelable(cancelavel)
            popup.setCanceledOnTouchOutside(cancelavel)

            popup.tvIcone.isVisible = tipo != null
            tipo?.let {
                popup.tvIcone.text = it.simbolo
                popup.tvIcone.backgroundTintList = ContextCompat.getColorStateList(context, it.cor)
            }
            popup.tvTitulo.text = titulo
            popup.tvMensagem.isVisible = !mensagem.isNullOrEmpty()
            popup.tvMensagem.text = mensagem
            conteudo?.let {
                val parametros = it.layoutParams
                    ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                popup.moldura.addView(it, parametros)
                popup.moldura.isVisible = true
            }

            popup.btnPrimario.text = botao
            if (tipo == Tipo.PERIGO) popup.btnPrimario.setBackgroundResource(R.drawable.bg_botao_perigo)
            popup.btnPrimario.setOnClickListener {
                if (validar()) {
                    popup.dismiss()
                    aoConfirmar()
                } else {
                    it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.tremer))
                }
            }

            popup.btnSecundario.isVisible = botaoSecundario != null
            popup.btnSecundario.text = botaoSecundario
            popup.btnSecundario.setOnClickListener {
                popup.dismiss()
                aoSecundario()
            }

            popup.show()
            return popup
        }

        /** Bottom sheet "câmera ou galeria" (RF17.3 foto de progresso / RF38.5 mídia do exercício). */
        fun escolherMidia(
            context: Context,
            titulo: String,
            aoEscolherCamera: () -> Unit,
            aoEscolherGaleria: () -> Unit,
        ) {
            val folha = BottomSheetDialog(context)
            folha.setContentView(R.layout.popup_escolher_midia)
            folha.findViewById<TextView>(R.id.tvTituloMidia)?.text = titulo
            folha.findViewById<View>(R.id.opcaoCamera)?.setOnClickListener {
                folha.dismiss()
                aoEscolherCamera()
            }
            folha.findViewById<View>(R.id.opcaoGaleria)?.setOnClickListener {
                folha.dismiss()
                aoEscolherGaleria()
            }
            folha.findViewById<View>(R.id.opcaoCancelar)?.setOnClickListener { folha.dismiss() }
            folha.show()
        }
    }
}
