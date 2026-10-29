package br.edu.unifor.uniforfit.ui.evolucao

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.core.dp
import br.edu.unifor.uniforfit.core.formatarDecimal
import br.edu.unifor.uniforfit.core.formatarPeso
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.FotoProgresso
import br.edu.unifor.uniforfit.ui.comum.GraficoLinhaView
import kotlin.math.abs

/** Evolução Corporal (RF17) — mesma aba "Evolução" da navbar. */
class EvolucaoCorporalActivity : BaseActivity() {

    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        foto?.let { adicionarFoto(FotoProgresso(bitmap = it)) }
    }
    private val galeria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { adicionarFoto(FotoProgresso(uri = it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComNavbar(R.layout.activity_evolucao_corporal, R.id.nav_evolucao)

        findViewById<RadioButton>(R.id.rbAbaCorporal).isChecked = true
        findViewById<RadioGroup>(R.id.rgAbasEvolucao).setOnCheckedChangeListener { _, id ->
            if (id == R.id.rbAbaCarga) {
                abrir(EvolucaoCargaActivity::class.java, Transicao.FADE)
                finish()
            }
        }
        // RF17.4
        findViewById<View>(R.id.btnRegistrarPeso).setOnClickListener {
            abrir(CalculadoraImcActivity::class.java)
        }
        // RF17.3 — câmera ou galeria
        findViewById<View>(R.id.btnAdicionarFoto).setOnClickListener {
            Popup.escolherMidia(
                this,
                titulo = "Adicionar foto de progresso",
                aoEscolherCamera = { tirarFoto() },
                aoEscolherGaleria = { galeria.launch("image/*") },
            )
        }
    }

    /** O peso pode ter mudado na Calculadora de IMC. */
    override fun onResume() {
        super.onResume()
        findViewById<GraficoLinhaView>(R.id.graficoPeso).valores = Dados.historicoPeso

        val variacao = Dados.historicoPeso.last() - Dados.historicoPeso.first()
        findViewById<TextView>(R.id.tvVariacaoPeso).apply {
            text = (if (variacao > 0) "+" else "-") + abs(variacao.toDouble()).formatarDecimal() + "kg"
            setTextColor(ContextCompat.getColor(context, if (variacao <= 0) R.color.verde else R.color.azul))
        }
        findViewById<TextView>(R.id.tvRotuloVariacao).text = if (variacao <= 0) "Peso perdido" else "Peso ganho"
        findViewById<TextView>(R.id.tvPesoAtual).text = "${Dados.pesoKg.formatarPeso()}kg"
        findViewById<TextView>(R.id.tvImcAtual).text = Dados.imc().formatarDecimal()
        montarFotos()
    }

    private fun montarFotos() {
        val lista = findViewById<LinearLayout>(R.id.listaFotos)
        lista.removeAllViews()
        val vagas = (MINIMO_DE_QUADROS - Dados.fotosProgresso.size).coerceAtLeast(0)
        (Dados.fotosProgresso.map { it as FotoProgresso? } + List(vagas) { null }).forEach { foto ->
            lista.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(104), dp(136)).apply { marginEnd = dp(8) }
                setBackgroundResource(R.drawable.bg_thumb)
                clipToOutline = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = if (foto == null) "Espaço para foto de progresso" else "Foto de progresso"
                foto?.bitmap?.let { setImageBitmap(it) }
                foto?.uri?.let { setImageURI(it) }
            })
        }
    }

    private fun tirarFoto() {
        try {
            camera.launch(null)
        } catch (e: ActivityNotFoundException) {
            avisar("Nenhum app de câmera encontrado neste aparelho.")
        }
    }

    private fun adicionarFoto(foto: FotoProgresso) {
        Dados.fotosProgresso.add(0, foto)
        montarFotos()
        avisar("Foto adicionada às suas fotos de progresso!")
    }

    private companion object {
        const val MINIMO_DE_QUADROS = 3
    }
}
