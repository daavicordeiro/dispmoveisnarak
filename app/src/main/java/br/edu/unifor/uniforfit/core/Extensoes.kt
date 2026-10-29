package br.edu.unifor.uniforfit.core

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import kotlin.math.roundToInt

/** Converte dp em pixels para views montadas no código. */
fun Context.dp(valor: Int): Int = (valor * resources.displayMetrics.density).roundToInt()

/** 72.0 → "72" · 72.5 → "72,5" */
fun Double.formatarPeso(): String =
    if (this % 1.0 == 0.0) toInt().toString() else formatarDecimal()

/** 23.51 → "23,5" */
fun Double.formatarDecimal(): String = String.format("%.1f", this).replace('.', ',')

fun Activity.esconderTeclado() {
    val foco = currentFocus ?: return
    getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(foco.windowToken, 0)
    foco.clearFocus()
}
