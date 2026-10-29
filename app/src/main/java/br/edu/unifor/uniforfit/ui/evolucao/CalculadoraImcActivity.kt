package br.edu.unifor.uniforfit.ui.evolucao

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.esconderTeclado
import br.edu.unifor.uniforfit.core.formatarDecimal
import br.edu.unifor.uniforfit.core.formatarPeso
import br.edu.unifor.uniforfit.data.Dados

/** Calculadora de IMC (RF18). O botão de voltar (RF11.1) é ligado pela BaseActivity. */
class CalculadoraImcActivity : BaseActivity() {

    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComAssistente(R.layout.activity_calculadora_imc)

        etPeso = findViewById(R.id.etPeso)
        etAltura = findViewById(R.id.etAltura)
        // RF18.1 — pré-preenchidos com os dados da anamnese
        if (savedInstanceState == null) {
            etPeso.setText(Dados.pesoKg.formatarPeso())
            etAltura.setText(Dados.alturaCm.toString())
        }
        etAltura.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) calcular()
            acao == EditorInfo.IME_ACTION_DONE
        }
        findViewById<View>(R.id.btnCalcular).setOnClickListener { calcular() }
    }

    private fun calcular() {
        val peso = etPeso.text.toString().replace(',', '.').toDoubleOrNull()
        val altura = etAltura.text.toString().toIntOrNull()
        if (peso == null || peso !in 30.0..300.0) {
            etPeso.error = "Peso inválido"
            etPeso.tremer()
            return
        }
        if (altura == null || altura !in 100..250) {
            etAltura.error = "Altura inválida"
            etAltura.tremer()
            return
        }
        esconderTeclado()

        // RF18.2 — valor em destaque e classificação
        val imc = Dados.imc(peso, altura)
        val (classificacao, cor) = Dados.classificacaoImc(imc)
        findViewById<View>(R.id.cardResultado).isVisible = true
        findViewById<TextView>(R.id.tvImc).text = imc.formatarDecimal()
        findViewById<TextView>(R.id.tvClassificacao).apply {
            text = classificacao
            backgroundTintList = ContextCompat.getColorStateList(context, cor)
        }

        // RF18.1 — um novo peso/altura passa a valer na evolução corporal
        if (peso != Dados.pesoKg || altura != Dados.alturaCm) {
            Dados.pesoKg = peso
            Dados.alturaCm = altura
            Dados.historicoPeso.add(peso.toFloat())
            avisar("Novo peso registrado na sua evolução corporal.")
        }
    }
}
