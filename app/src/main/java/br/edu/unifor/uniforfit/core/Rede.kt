package br.edu.unifor.uniforfit.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Verificação de internet usada para decidir quando mostrar a tela "Sem conexão" (RF23). */
object Rede {

    fun estaOnline(context: Context): Boolean {
        val gerenciador = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val capacidades = gerenciador.getNetworkCapabilities(gerenciador.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
