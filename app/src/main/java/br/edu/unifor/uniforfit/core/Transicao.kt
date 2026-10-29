package br.edu.unifor.uniforfit.core

import androidx.annotation.AnimRes
import br.edu.unifor.uniforfit.R

/**
 * Animações usadas na ligação entre as telas (RNF03.1 — transições abaixo de 1 segundo).
 * Cada tela guarda a transição com que foi aberta e usa a inversa ao ser fechada.
 */
enum class Transicao(
    @AnimRes val abrirEntrada: Int,
    @AnimRes val abrirSaida: Int,
    @AnimRes val fecharEntrada: Int,
    @AnimRes val fecharSaida: Int,
) {
    /** Avançar no fluxo: a tela nova entra pela direita e, ao voltar, sai pela direita. */
    AVANCAR(R.anim.entrar_direita, R.anim.sair_esquerda, R.anim.entrar_esquerda, R.anim.sair_direita),

    /** Troca de abas da navbar, saída da splash, login → início, sair da conta. */
    FADE(R.anim.fade_entrar, R.anim.fade_sair, R.anim.fade_entrar, R.anim.fade_sair),

    /** Telas "modais" que sobem por cima da atual (assistente, execução do treino). */
    SUBIR(R.anim.subir, R.anim.parado, R.anim.parado, R.anim.descer),
}
