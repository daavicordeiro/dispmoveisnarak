package br.edu.unifor.uniforfit.ui.onboarding

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.formatarPeso
import br.edu.unifor.uniforfit.data.Dados

/**
 * Anamnese — Etapa 1 de 3 (RF05).
 * Com [EXTRA_MODO_EDICAO] vem preenchida, a partir do Perfil (RF21.2), e o fluxo termina de volta nele.
 */
class AnamneseActivity : BaseActivity() {

    private val modoEdicao by lazy { intent.getBooleanExtra(EXTRA_MODO_EDICAO, false) }

    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText
    private lateinit var etLesao: EditText
    private lateinit var etObjetivo: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anamnese)

        etPeso = findViewById(R.id.etPeso)
        etAltura = findViewById(R.id.etAltura)
        etLesao = findViewById(R.id.etLesao)
        etObjetivo = findViewById(R.id.etObjetivo)

        if (modoEdicao && savedInstanceState == null) {
            findViewById<TextView>(R.id.tvVoltar).isVisible = true
            etPeso.setText(Dados.pesoKg.formatarPeso())
            etAltura.setText(Dados.alturaCm.toString())
            etLesao.setText(Dados.lesao)
            etObjetivo.setText(Dados.objetivo)
        }

        findViewById<TextView>(R.id.btnContinuar).setOnClickListener { continuar() }
    }

    private fun continuar() {
        val peso = etPeso.text.toString().replace(',', '.').toDoubleOrNull()
        val altura = etAltura.text.toString().toIntOrNull()
        val objetivo = etObjetivo.text.toString().trim()

        var valido = true
        if (peso == null || peso !in 30.0..300.0) {
            etPeso.error = "Informe um peso entre 30 e 300 kg"
            valido = false
        }
        if (altura == null || altura !in 100..250) {
            etAltura.error = "Informe a altura em centímetros"
            valido = false
        }
        if (objetivo.isEmpty()) {
            etObjetivo.error = "Conte qual é o seu objetivo principal"
            valido = false
        }
        if (!valido || peso == null || altura == null) {
            findViewById<TextView>(R.id.btnContinuar).tremer()
            return
        }

        Dados.pesoKg = peso
        Dados.alturaCm = altura
        Dados.lesao = etLesao.text.toString().trim()
        Dados.objetivo = objetivo

        // RF05.2 — avança para a etapa 2
        abrir(ParqActivity::class.java) { putExtra(EXTRA_MODO_EDICAO, modoEdicao) }
    }

    companion object {
        const val EXTRA_MODO_EDICAO = "modo_edicao"
    }
}
