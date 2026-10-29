package br.edu.unifor.uniforfit.ui.treino

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.Treino
import br.edu.unifor.uniforfit.ui.home.HomeActivity

/**
 * Execução da Série (RF14), com o intervalo de descanso e o "Treino concluído!" (RF15) em pop-ups.
 * Os tempos são calculados a partir do relógio do sistema, então continuam certos mesmo
 * com o app em segundo plano ou a tela bloqueada (RF14.4 / RNF03.3).
 */
class ExecucaoSerieActivity : BaseActivity() {

    override val telaEscura = true

    private lateinit var treino: Treino
    private lateinit var tvCronometro: TextView
    private lateinit var btnPausar: TextView

    private var indiceItem = 0
    private var serie = 1
    private var inicioTreino = 0L
    private var fimSerie = 0L
    private var restantePausado = 0L
    private var pausado = false
    private var fimDescanso = 0L
    private var popupDescanso: Popup? = null
    private var concluido = false

    private val tique = object : Runnable {
        override fun run() {
            atualizarTempos()
            tvCronometro.postDelayed(this, 250)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        treino = Dados.treino(intent.getIntExtra(DetalheTreinoActivity.EXTRA_TREINO, 0))
            ?.takeIf { it.itens.isNotEmpty() }
            ?: run {
                finish()
                return
            }
        setContentView(R.layout.activity_execucao_serie)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tvCronometro = findViewById(R.id.tvCronometro)
        btnPausar = findViewById(R.id.btnPausar)
        btnPausar.setOnClickListener { alternarPausa() }
        findViewById<TextView>(R.id.btnConcluirSerie).setOnClickListener { concluirSerie() }
        onBackPressedDispatcher.addCallback(this) { confirmarEncerrar() }

        val agora = SystemClock.elapsedRealtime()
        if (savedInstanceState == null) {
            indiceItem = intent.getIntExtra(DetalheTreinoActivity.EXTRA_ITEM, 0).coerceIn(treino.itens.indices)
            inicioTreino = agora
            iniciarSerie()
        } else {
            indiceItem = savedInstanceState.getInt("item")
            serie = savedInstanceState.getInt("serie")
            inicioTreino = savedInstanceState.getLong("inicio")
            fimSerie = savedInstanceState.getLong("fimSerie")
            restantePausado = savedInstanceState.getLong("restantePausado")
            pausado = savedInstanceState.getBoolean("pausado")
            fimDescanso = savedInstanceState.getLong("fimDescanso")
            mostrarSerie()
            btnPausar.text = if (pausado) "Retomar" else "Pausar"
            if (fimDescanso > 0) mostrarDescanso()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!concluido) tvCronometro.post(tique)
    }

    override fun onPause() {
        tvCronometro.removeCallbacks(tique)
        super.onPause()
    }

    override fun onDestroy() {
        popupDescanso?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("item", indiceItem)
        outState.putInt("serie", serie)
        outState.putLong("inicio", inicioTreino)
        outState.putLong("fimSerie", fimSerie)
        outState.putLong("restantePausado", restantePausado)
        outState.putBoolean("pausado", pausado)
        outState.putLong("fimDescanso", fimDescanso)
    }

    // ---------- Série ----------

    private fun iniciarSerie() {
        fimSerie = SystemClock.elapsedRealtime() + DURACAO_SERIE_MS
        pausado = false
        btnPausar.text = "Pausar"
        mostrarSerie()
        atualizarTempos()
    }

    private fun mostrarSerie() {
        val item = treino.itens[indiceItem]
        findViewById<TextView>(R.id.tvExercicioSerie).text =
            "${item.exercicio.nome} · Série $serie de ${item.series}"
        findViewById<TextView>(R.id.tvMaquinaSerie).text = item.exercicio.maquina
        findViewById<TextView>(R.id.tvRepsSerie).text = item.repeticoes.toString()
        findViewById<TextView>(R.id.tvCargaSerie).text = item.cargaTexto
        findViewById<TextView>(R.id.tvProgressoTreino).text = "Exercício ${indiceItem + 1} de ${treino.itens.size}"
        findViewById<TextView>(R.id.tvProximo).text =
            treino.itens.getOrNull(indiceItem + 1)?.let { "Próximo: ${it.exercicio.nome}" } ?: "Último exercício do treino"
    }

    /** RF14.2 — pausa sem encerrar a série. */
    private fun alternarPausa() {
        val agora = SystemClock.elapsedRealtime()
        if (pausado) {
            fimSerie = agora + restantePausado
            pausado = false
            btnPausar.text = "Pausar"
        } else {
            restantePausado = (fimSerie - agora).coerceAtLeast(0)
            pausado = true
            btnPausar.text = "Retomar"
        }
        atualizarTempos()
    }

    private fun atualizarTempos() {
        val agora = SystemClock.elapsedRealtime()
        val restante = if (pausado) restantePausado else (fimSerie - agora).coerceAtLeast(0)
        tvCronometro.text = formatar(restante)
        tvCronometro.setTextColor(ContextCompat.getColor(this, if (restante == 0L) R.color.verde else R.color.branco))
        tvCronometro.alpha = if (pausado) 0.5f else 1f

        if (fimDescanso > 0) {
            val restanteDescanso = (fimDescanso - agora).coerceAtLeast(0)
            popupDescanso?.atualizar(titulo = "Descanso · ${formatar(restanteDescanso)}")
            if (restanteDescanso == 0L) {
                popupDescanso?.dismiss()
                terminarDescanso()
            }
        }
    }

    /** RF14.3 — registra a série e vai para o intervalo (ou encerra o treino na última série). */
    private fun concluirSerie() {
        val item = treino.itens[indiceItem]
        val ultimaSerie = serie >= item.series
        if (ultimaSerie && indiceItem == treino.itens.lastIndex) {
            concluirTreino()
            return
        }
        fimDescanso = SystemClock.elapsedRealtime() + item.descansoSeg * 1000L
        mostrarDescanso()
    }

    // ---------- Pop-up: intervalo de descanso ----------

    private fun mostrarDescanso() {
        val item = treino.itens[indiceItem]
        val proxima = if (serie >= item.series) {
            treino.itens[indiceItem + 1].let { "${it.exercicio.nome} · Série 1 de ${it.series}" }
        } else {
            "${item.exercicio.nome} · Série ${serie + 1} de ${item.series}"
        }
        popupDescanso = Popup.mostrar(
            this,
            titulo = "Descanso · ${formatar((fimDescanso - SystemClock.elapsedRealtime()).coerceAtLeast(0))}",
            mensagem = "Respire e hidrate-se.\nPróximo: $proxima",
            tipo = Popup.Tipo.TEMPO,
            botao = "Pular descanso",
            cancelavel = false,
        ) { terminarDescanso() }
    }

    private fun terminarDescanso() {
        if (fimDescanso == 0L) return
        fimDescanso = 0L
        popupDescanso = null
        val item = treino.itens[indiceItem]
        if (serie >= item.series) {
            indiceItem++
            serie = 1
        } else {
            serie++
        }
        iniciarSerie()
    }

    // ---------- Pop-ups: encerrar e concluir ----------

    private fun confirmarEncerrar() {
        Popup.mostrar(
            this,
            titulo = "Encerrar treino?",
            mensagem = "O progresso desta sessão não será salvo e você não ganhará os pontos do ranking.",
            tipo = Popup.Tipo.PERIGO,
            botao = "Encerrar",
            botaoSecundario = "Continuar treinando",
        ) { finish() }
    }

    /** RF15 — pop-up "Treino concluído!" com resumo e pontos; "Concluir" grava e volta ao Início. */
    private fun concluirTreino() {
        concluido = true
        tvCronometro.removeCallbacks(tique)
        val minutos = ((SystemClock.elapsedRealtime() - inicioTreino) / 60_000).coerceAtLeast(1)
        Popup.mostrar(
            this,
            titulo = "Treino concluído!",
            mensagem = "Você completou o ${treino.nome} em $minutos min.\n+${Dados.PONTOS_POR_TREINO} pontos no ranking!",
            tipo = Popup.Tipo.SUCESSO,
            botao = "Concluir",
            cancelavel = false,
        ) {
            Dados.registrarTreinoConcluido(treino)
            voltarPara(HomeActivity::class.java)
        }
    }

    private fun formatar(ms: Long): String {
        val segundos = (ms + 999) / 1000
        return "%02d:%02d".format(segundos / 60, segundos % 60)
    }

    private companion object {
        const val DURACAO_SERIE_MS = 45_000L
    }
}
