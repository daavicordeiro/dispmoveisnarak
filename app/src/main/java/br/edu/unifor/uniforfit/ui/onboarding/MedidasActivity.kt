package br.edu.unifor.uniforfit.ui.onboarding

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.core.Transicao
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.perfil.PerfilActivity

/** Medidas Corporais — Etapa 3 de 3 (RF07). */
class MedidasActivity : BaseActivity() {

    private val modoEdicao by lazy { intent.getBooleanExtra(AnamneseActivity.EXTRA_MODO_EDICAO, false) }

    private lateinit var etCintura: EditText
    private lateinit var etQuadril: EditText
    private lateinit var etBraco: EditText
    private lateinit var etCoxa: EditText
    private lateinit var etGordura: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medidas)

        etCintura = findViewById(R.id.etCintura)
        etQuadril = findViewById(R.id.etQuadril)
        etBraco = findViewById(R.id.etBraco)
        etCoxa = findViewById(R.id.etCoxa)
        etGordura = findViewById(R.id.etGordura)

        val btnConcluir = findViewById<TextView>(R.id.btnConcluir)
        if (modoEdicao) {
            btnConcluir.text = "Salvar alterações"
            if (savedInstanceState == null) {
                etCintura.setText(Dados.cintura)
                etQuadril.setText(Dados.quadril)
                etBraco.setText(Dados.braco)
                etCoxa.setText(Dados.coxa)
                etGordura.setText(Dados.gordura)
            }
        }
        btnConcluir.setOnClickListener { concluir() }
    }

    private fun concluir() {
        val obrigatorios = listOf(etCintura, etQuadril, etBraco, etCoxa)
        val vazios = obrigatorios.filter { it.text.toString().replace(',', '.').toDoubleOrNull() == null }
        vazios.forEach { it.error = "Obrigatório" }
        if (vazios.isNotEmpty()) {
            findViewById<TextView>(R.id.btnConcluir).tremer()
            return
        }

        Dados.cintura = etCintura.text.toString()
        Dados.quadril = etQuadril.text.toString()
        Dados.braco = etBraco.text.toString()
        Dados.coxa = etCoxa.text.toString()
        Dados.gordura = etGordura.text.toString()

        // RF07.2 — encerra o onboarding (→ Apresentação) ou volta ao Perfil quando era edição
        if (modoEdicao) {
            Popup.mostrar(
                this,
                titulo = "Dados atualizados!",
                mensagem = "Sua anamnese, o PAR-Q e as medidas foram salvos. Seu professor já vê as novas informações.",
                botao = "Voltar ao perfil",
                cancelavel = false,
            ) { voltarPara(PerfilActivity::class.java) }
        } else {
            Dados.onboardingPendente -= Dados.raAluno
            abrirComoInicio(ApresentacaoActivity::class.java, Transicao.FADE)
        }
    }
}
