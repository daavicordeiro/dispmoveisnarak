package br.edu.unifor.uniforfit.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.ui.chatbot.ChatbotActivity
import br.edu.unifor.uniforfit.ui.comum.SemConexaoActivity
import br.edu.unifor.uniforfit.ui.evolucao.EvolucaoCargaActivity
import br.edu.unifor.uniforfit.ui.home.HomeActivity
import br.edu.unifor.uniforfit.ui.perfil.PerfilActivity
import br.edu.unifor.uniforfit.ui.ranking.RankingActivity
import br.edu.unifor.uniforfit.ui.treino.MinhaSemanaActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

/**
 * Base de todas as telas. Centraliza a ligação entre elas:
 *  - transição de entrada/saída (ver [Transicao]);
 *  - navbar do aluno (RF09.6), botão do assistente (RF09.5) e links "← Voltar" (RF11.1);
 *  - tela "Sem conexão" com "Tentar novamente" repetindo a última ação (RF23);
 *  - tamanho de fonte escolhido em Acessibilidade (RF24).
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Telas de fundo escuro (execução da série, login do gestor...) usam ícones claros na barra de status. */
    protected open val telaEscura = false

    private lateinit var transicao: Transicao
    private var fonteAplicada = TamanhoFonte.NORMAL
    private var acaoAguardandoConexao: (() -> Unit)? = null

    /** Layout da tela quando ela vai dentro de uma moldura (uf_shell_aluno / uf_shell_fab). */
    private var conteudoInflado: View? = null

    private val telaSemConexao =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            val acao = acaoAguardandoConexao
            acaoAguardandoConexao = null
            if (resultado.resultCode == RESULT_OK) acao?.invoke()
        }

    override fun attachBaseContext(novoBase: Context) {
        fonteAplicada = Preferencias.tamanhoFonte(novoBase)
        super.attachBaseContext(Preferencias.aplicarTamanhoFonte(novoBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transicao = intent.getStringExtra(EXTRA_TRANSICAO)
            ?.let { nome -> Transicao.entries.firstOrNull { it.name == nome } }
            ?: Transicao.AVANCAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, transicao.abrirEntrada, transicao.abrirSaida)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, transicao.fecharEntrada, transicao.fecharSaida)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val raiz = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        aplicarMargensDoSistema(raiz)
        WindowCompat.getInsetsController(window, raiz).apply {
            isAppearanceLightStatusBars = !telaEscura
            isAppearanceLightNavigationBars = !telaEscura
        }

        // Ligações comuns: só entram se a própria tela não tiver definido outra ação.
        listOf(R.id.tvVoltar, R.id.btnVoltar).forEach { id ->
            findViewById<View>(id)?.let { botao ->
                if (!botao.hasOnClickListeners()) botao.setOnClickListener { voltar() }
            }
        }
        listOf(R.id.fabAssistente, R.id.btnChatbot).forEach { id ->
            findViewById<View>(id)?.let { botao ->
                if (!botao.hasOnClickListeners()) {
                    botao.setOnClickListener { abrir(ChatbotActivity::class.java, Transicao.SUBIR) }
                }
            }
        }
    }

    /** Telas que voltam com [voltarPara] podem trazer um aviso de confirmação ("Aluno salvo"...). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_AVISO)?.let { avisar(it) }
    }

    override fun onResume() {
        super.onResume()
        // Se o tamanho da fonte mudou em Acessibilidade, as telas da pilha se redesenham ao voltar.
        if (Preferencias.tamanhoFonte(this) != fonteAplicada) recreate()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ::transicao.isInitialized) {
            @Suppress("DEPRECATION")
            overridePendingTransition(transicao.fecharEntrada, transicao.fecharSaida)
        }
    }

    /**
     * O app desenha atrás das barras do sistema (padrão do Android 15+). Aqui a raiz de cada tela
     * ganha, além do padding que já tinha, o espaço da barra de status, da navegação e do teclado.
     */
    private fun aplicarMargensDoSistema(raiz: View) {
        // Nas molduras (navbar/assistente) o espaço vai no conteúdo, para o fundo dele cobrir a barra de status.
        val alvo = conteudoInflado ?: raiz
        val navbar = findViewById<View>(R.id.bottomNav)
        val fabDaMoldura = if (conteudoInflado != null && navbar == null) findViewById<View>(R.id.fabAssistente) else null

        val padding = intArrayOf(alvo.paddingLeft, alvo.paddingTop, alvo.paddingRight, alvo.paddingBottom)
        val paddingNavbar = navbar?.paddingBottom ?: 0
        val margemFab = (fabDaMoldura?.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            // Com navbar, ela é que se estende por trás da barra de gestos.
            val baseNoConteudo = if (navbar == null) barras.bottom else 0
            alvo.setPadding(
                padding[0] + barras.left, padding[1] + barras.top,
                padding[2] + barras.right, padding[3] + baseNoConteudo,
            )
            navbar?.setPadding(navbar.paddingLeft, navbar.paddingTop, navbar.paddingRight, paddingNavbar + barras.bottom)
            fabDaMoldura?.let { fab ->
                (fab.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = margemFab + barras.bottom
                fab.requestLayout()
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(raiz)
    }

    // ---------- Montagem das telas ----------

    /** Tela principal do aluno: conteúdo + assistente + navbar com [aba] em destaque. */
    protected fun setConteudoComNavbar(@LayoutRes layout: Int, @IdRes aba: Int) {
        setContentView(R.layout.uf_shell_aluno)
        inflarNaMoldura(layout)
        configurarNavbar(aba)
    }

    /** Tela interna do aluno: conteúdo + botão flutuante do assistente. */
    protected fun setConteudoComAssistente(@LayoutRes layout: Int) {
        setContentView(R.layout.uf_shell_fab)
        inflarNaMoldura(layout)
    }

    private fun inflarNaMoldura(@LayoutRes layout: Int) {
        val moldura = findViewById<FrameLayout>(R.id.conteudo)
        val conteudo = layoutInflater.inflate(layout, moldura, false)
        moldura.addView(conteudo, 0)
        conteudoInflado = conteudo
    }

    /** Liga a navbar (Início, Treinos, Evolução, Ranking, Perfil) de telas que já trazem a sua própria. */
    protected fun configurarNavbar(@IdRes aba: Int) {
        val navbar = findViewById<BottomNavigationView>(R.id.bottomNav) ?: return
        navbar.selectedItemId = aba
        navbar.setOnItemSelectedListener { item ->
            if (item.itemId != aba) irParaAba(item.itemId)
            false
        }
    }

    /**
     * O Início fica sempre na base da pilha; as outras abas substituem umas às outras.
     * Assim o "voltar" de qualquer aba leva ao Início, como no padrão do Android.
     */
    private fun irParaAba(@IdRes aba: Int) {
        val destino = when (aba) {
            R.id.nav_treinos -> MinhaSemanaActivity::class.java
            R.id.nav_evolucao -> EvolucaoCargaActivity::class.java
            R.id.nav_ranking -> RankingActivity::class.java
            R.id.nav_perfil -> PerfilActivity::class.java
            else -> HomeActivity::class.java
        }
        if (destino == HomeActivity::class.java) {
            abrir(destino, Transicao.FADE, Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return
        }
        abrir(destino, Transicao.FADE)
        if (this !is HomeActivity) finish()
    }

    // ---------- Navegação ----------

    fun abrir(
        destino: Class<out Activity>,
        transicao: Transicao = Transicao.AVANCAR,
        flags: Int = 0,
        extras: Intent.() -> Unit = {},
    ) {
        val intent = Intent(this, destino)
            .putExtra(EXTRA_TRANSICAO, transicao.name)
            .addFlags(flags)
        intent.extras()
        startActivity(intent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(transicao.abrirEntrada, transicao.abrirSaida)
        }
    }

    /** Abre [destino] como nova base da pilha (login → início, sair da conta...). */
    fun abrirComoInicio(
        destino: Class<out Activity>,
        transicao: Transicao = Transicao.FADE,
        extras: Intent.() -> Unit = {},
    ) = abrir(destino, transicao, Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, extras)

    /** Volta para uma tela que já está na pilha, fechando as que estão acima dela. */
    fun voltarPara(destino: Class<out Activity>, extras: Intent.() -> Unit = {}) {
        val intent = Intent(this, destino)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.extras()
        startActivity(intent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.entrar_esquerda, R.anim.sair_direita)
        }
    }

    /** Mesmo efeito do botão "voltar" do sistema (respeita confirmações registradas pela tela). */
    fun voltar() = onBackPressedDispatcher.onBackPressed()

    /** RF23 — sem internet abre "Sem conexão"; ao reconectar, [acao] é repetida automaticamente. */
    protected fun exigirConexao(acao: () -> Unit) {
        if (Rede.estaOnline(this)) {
            acao()
            return
        }
        acaoAguardandoConexao = acao
        telaSemConexao.launch(
            Intent(this, SemConexaoActivity::class.java)
                .putExtra(EXTRA_TRANSICAO, Transicao.AVANCAR.name)
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.entrar_direita, R.anim.sair_esquerda)
        }
    }

    // ---------- Retornos rápidos ----------

    /** Aviso curto no rodapé (ações simples que não precisam de pop-up). */
    fun avisar(texto: String) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), texto, Snackbar.LENGTH_SHORT)
        findViewById<View>(R.id.bottomNav)?.let { snackbar.anchorView = it }
        snackbar.show()
    }

    /** Balança o campo/botão quando a validação falha. */
    protected fun View.tremer() = startAnimation(AnimationUtils.loadAnimation(context, R.anim.tremer))

    companion object {
        const val EXTRA_TRANSICAO = "transicao"
        const val EXTRA_AVISO = "aviso"
    }
}
