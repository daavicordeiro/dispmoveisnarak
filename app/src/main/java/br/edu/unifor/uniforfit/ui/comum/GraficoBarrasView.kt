package br.edu.unifor.uniforfit.ui.comum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R

/** Gráfico de barras da carga por sessão, com a sessão mais recente destacada (RF16.1 / RF31.9). */
class GraficoBarrasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Carga (kg) de cada sessão, da mais antiga para a mais recente. */
    var valores: List<Float> = emptyList()
        set(novos) {
            field = novos.takeLast(MAX_BARRAS)
            primeiraSessao = (novos.size - field.size) + 1
            contentDescription = if (field.isEmpty()) "Sem dados de carga" else
                "Carga por sessão: " + field.joinToString(", ") { "${it.toInt()} quilos" }
            invalidate()
        }

    private var primeiraSessao = 1
    private val retangulo = RectF()
    private val raio = dp(8f)
    private val espaco = dp(8f)

    private val pincelBarra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.azul_claro)
    }
    private val pincelDestaque = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.azul)
    }
    private val pincelRotulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.texto_cinza)
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }
    private val pincelValor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.azul)
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (valores.isEmpty()) return

        val alturaRotulo = dp(20f)
        val alturaValor = dp(18f)
        val base = height - paddingBottom - alturaRotulo
        val alturaUtil = base - paddingTop - alturaValor
        val largura = (width - paddingLeft - paddingRight - espaco * (valores.size - 1)) / valores.size
        val maximo = valores.max().coerceAtLeast(1f)

        valores.forEachIndexed { i, valor ->
            val esquerda = paddingLeft + i * (largura + espaco)
            val altura = alturaUtil * (0.15f + 0.85f * valor / maximo)
            retangulo.set(esquerda, base - altura, esquerda + largura, base)
            val ultima = i == valores.lastIndex
            canvas.drawRoundRect(retangulo, raio, raio, if (ultima) pincelDestaque else pincelBarra)
            canvas.drawText("S${primeiraSessao + i}", esquerda + largura / 2, height - paddingBottom - dp(4f), pincelRotulo)
            if (ultima) canvas.drawText("${valor.toInt()}kg", esquerda + largura / 2, base - altura - dp(5f), pincelValor)
        }
    }

    private fun dp(valor: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor, resources.displayMetrics)

    private fun sp(valor: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, valor, resources.displayMetrics)

    private companion object {
        const val MAX_BARRAS = 8
    }
}
