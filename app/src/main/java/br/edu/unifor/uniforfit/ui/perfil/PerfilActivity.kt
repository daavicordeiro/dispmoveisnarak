package br.edu.unifor.uniforfit.ui.perfil

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.auth.LoginActivity
import br.edu.unifor.uniforfit.ui.onboarding.AnamneseActivity
import br.edu.unifor.uniforfit.ui.professor.FaleProfessorActivity

/** Perfil do Aluno (RF21) — aba "Perfil" da navbar. */
class PerfilActivity : BaseActivity() {

    private lateinit var ivAvatar: ImageView

    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        foto?.let { ivAvatar.setImageBitmap(it) }
    }
    private val galeria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { ivAvatar.setImageURI(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uf_activity_perfil)
        configurarNavbar(R.id.nav_perfil)

        ivAvatar = findViewById(R.id.ivAvatar)
        ivAvatar.clipToOutline = true
        ivAvatar.setOnClickListener {
            Popup.escolherMidia(
                this,
                titulo = "Foto de perfil",
                aoEscolherCamera = {
                    try {
                        camera.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        avisar("Nenhum app de câmera encontrado neste aparelho.")
                    }
                },
                aoEscolherGaleria = { galeria.launch("image/*") },
            )
        }

        // RF21.2 — reabre a anamnese preenchida (e segue para PAR-Q e medidas)
        findViewById<View>(R.id.itemEditarAnamnese).setOnClickListener {
            abrir(AnamneseActivity::class.java) { putExtra(AnamneseActivity.EXTRA_MODO_EDICAO, true) }
        }
        // RF21.3
        findViewById<View>(R.id.itemFaleProfessor).setOnClickListener {
            abrir(FaleProfessorActivity::class.java)
        }
        // RF21.4
        findViewById<View>(R.id.itemAcessibilidade).setOnClickListener {
            abrir(AcessibilidadeActivity::class.java)
        }
        // RF21.5 — confirmação antes de encerrar a sessão
        findViewById<View>(R.id.itemSair).setOnClickListener {
            Popup.mostrar(
                this,
                titulo = getString(R.string.uf_sair_confirmar_titulo),
                mensagem = getString(R.string.uf_sair_confirmar_msg),
                tipo = Popup.Tipo.PERIGO,
                botao = getString(R.string.uf_sair),
                botaoSecundario = getString(R.string.uf_cancelar),
            ) { abrirComoInicio(LoginActivity::class.java) }
        }
    }

    /** RF21.1 — indicadores atualizados depois de cada treino. */
    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.tvNome).text = Dados.nomeAluno
        findViewById<TextView>(R.id.tvRa).text = getString(R.string.uf_ra_formato, Dados.raAluno)
        preencherIndicador(R.id.statRanking, getString(R.string.uf_ranking_formato, Dados.posicaoRanking), R.string.uf_rotulo_ranking)
        preencherIndicador(R.id.statTreinos, Dados.treinosTotais.toString(), R.string.uf_rotulo_treinos)
        preencherIndicador(R.id.statPontos, Dados.pontos.toString(), R.string.uf_rotulo_pontos)
    }

    private fun preencherIndicador(idCard: Int, valor: String, rotulo: Int) {
        val card = findViewById<View>(idCard)
        card.findViewById<TextView>(R.id.tvValor).text = valor
        card.findViewById<TextView>(R.id.tvRotulo).setText(rotulo)
    }
}
