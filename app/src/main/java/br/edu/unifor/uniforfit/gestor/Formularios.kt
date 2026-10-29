package br.edu.unifor.uniforfit.gestor

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.dp

/**
 * Formulários do gestor (Editar Aluno/Treino/Exercício): ao voltar com alterações não salvas,
 * pergunta antes de descartar.
 */
abstract class FormularioActivity : BaseActivity() {

    private var estadoSalvo: List<Any?>? = null

    /** Valores atuais dos campos, comparados com os do último [marcarComoSalvo]. */
    protected abstract fun estadoAtual(): List<Any?>

    protected fun marcarComoSalvo() {
        estadoSalvo = estadoAtual()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) { sairComConfirmacao() }
    }

    private fun sairComConfirmacao() {
        if (estadoSalvo == null || estadoSalvo == estadoAtual()) {
            finish()
            return
        }
        Popup.mostrar(
            this,
            titulo = "Descartar alterações?",
            mensagem = "As mudanças feitas neste formulário ainda não foram salvas.",
            tipo = Popup.Tipo.ALERTA,
            botao = "Descartar",
            botaoSecundario = "Continuar editando",
        ) { finish() }
    }
}

/** Preenche uma lista suspensa com o visual dos campos do app. */
fun Spinner.preencher(opcoes: List<String>, selecionado: String?) {
    adapter = ArrayAdapter(context, R.layout.item_lista_suspensa, opcoes)
    setSelection(opcoes.indexOf(selecionado).coerceAtLeast(0))
}

val Spinner.valor: String get() = selectedItem as String

/** Campos de texto para o conteúdo de um pop-up. O segundo campo é opcional. */
class CamposPopup(contexto: Context) {
    val view: View = LayoutInflater.from(contexto).inflate(R.layout.popup_formulario, null, false)
    val campo1: EditText = view.findViewById(R.id.popupCampo1)
    val campo2: EditText = view.findViewById(R.id.popupCampo2)

    fun configurar(
        rotulo1: String,
        valor1: String = "",
        dica1: String = "",
        rotulo2: String? = null,
        valor2: String = "",
        dica2: String = "",
    ): CamposPopup {
        view.findViewById<TextView>(R.id.popupRotulo1).text = rotulo1
        campo1.setText(valor1)
        campo1.hint = dica1
        val temSegundo = rotulo2 != null
        view.findViewById<TextView>(R.id.popupRotulo2).isVisible = temSegundo
        campo2.isVisible = temSegundo
        view.findViewById<TextView>(R.id.popupRotulo2).text = rotulo2
        campo2.setText(valor2)
        campo2.hint = dica2
        return this
    }
}

/** Lista de caixas de marcação para pop-ups (ex.: escolher exercícios do catálogo). */
class MarcacoesPopup(contexto: Context, opcoes: List<String>, marcadas: List<Boolean> = emptyList()) {
    val caixas: List<CheckBox>
    val view: View

    init {
        val coluna = LinearLayout(contexto).apply { orientation = LinearLayout.VERTICAL }
        caixas = opcoes.mapIndexed { i, texto ->
            CheckBox(contexto).apply {
                text = texto
                textSize = 14f
                isChecked = marcadas.getOrElse(i) { false }
                minHeight = contexto.dp(44)
                buttonTintList = ContextCompat.getColorStateList(contexto, R.color.azul)
                setTextColor(ContextCompat.getColor(contexto, R.color.texto_titulo))
            }.also { coluna.addView(it) }
        }
        view = ScrollView(contexto).apply {
            addView(coluna)
            // Listas longas rolam dentro do pop-up
            if (opcoes.size > 6) {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contexto.dp(264))
            }
        }
    }

    val marcadas get() = caixas.map { it.isChecked }
}
