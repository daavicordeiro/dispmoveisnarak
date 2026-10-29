package br.edu.unifor.uniforfit.ui.comum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R

/** Gráfico de linha do peso corporal nos últimos três meses (RF17 / RF31.9.1). */
class GraficoLinhaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var valores: List<Float> = emptyList()
        set(novos) {
            field = novos
            contentDescription = if (novos.isEmpty()) "Sem registros de peso" else
                "Peso corporal de ${formatar(novos.first())} para ${formatar(novos.last())} quilos"
            invalidate()
        }

    private val caminho = Path()

    private val pincelLinha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.verde)
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pincelPonto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.verde)
    }
    private val pincelGuia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.borda)
        strokeWidth = dp(1f)
    }
    private val pincelTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.texto_cinza)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (valores.size < 2) return

        val margem = dp(8f)
        val esquerda = paddingLeft + margem
        val direita = width - paddingRight - dp(36f)
        val topo = paddingTop + margem
        val base = height - paddingBottom - margem
        val minimo = valores.min()
        val faixa = (valores.max() - minimo).coerceAtLeast(0.5f)

        canvas.drawLine(esquerda, base, direita, base, pincelGuia)
        canvas.drawText(formatar(valores.max()), direita + dp(6f), topo + dp(4f), pincelTexto)
        canvas.drawText(formatar(minimo), direita + dp(6f), base, pincelTexto)

        caminho.reset()
        valores.forEachIndexed { i, valor ->
            val x = esquerda + (direita - esquerda) * i / (valores.size - 1)
            val y = base - (base - topo) * (valor - minimo) / faixa
            if (i == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
            if (i == valores.lastIndex) canvas.drawCircle(x, y, dp(4f), pincelPonto)
        }
        canvas.drawPath(caminho, pincelLinha)
    }

    private fun formatar(valor: Float) = String.format("%.1f", valor).replace('.', ',')

    private fun dp(valor: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor, resources.displayMetrics)
}
