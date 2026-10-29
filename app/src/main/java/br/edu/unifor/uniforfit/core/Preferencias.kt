package br.edu.unifor.uniforfit.core

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

/** Opções da tela Acessibilidade (RF24 / RF41). */
enum class TamanhoFonte(val rotulo: String, val escala: Float) {
    PEQUENA("Pequena", 0.85f),
    NORMAL("Normal", 1.0f),
    GRANDE("Grande", 1.15f),
    ENORME("Enorme", 1.3f),
}

/** Tamanho da fonte escolhido em Acessibilidade, que vale para o app todo. */
object Preferencias {

    private const val ARQUIVO = "uniforfit"
    private const val CHAVE_FONTE = "tamanho_fonte"

    private fun prefs(context: Context) =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun tamanhoFonte(context: Context): TamanhoFonte =
        prefs(context).getString(CHAVE_FONTE, null)
            ?.let { nome -> TamanhoFonte.entries.firstOrNull { it.name == nome } }
            ?: TamanhoFonte.NORMAL

    fun salvarTamanhoFonte(context: Context, tamanho: TamanhoFonte) =
        prefs(context).edit { putString(CHAVE_FONTE, tamanho.name) }

    /** Contexto com a escala de fonte escolhida aplicada sobre a do sistema. */
    fun aplicarTamanhoFonte(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        config.fontScale = base.resources.configuration.fontScale * tamanhoFonte(base).escala
        return base.createConfigurationContext(config)
    }
}
