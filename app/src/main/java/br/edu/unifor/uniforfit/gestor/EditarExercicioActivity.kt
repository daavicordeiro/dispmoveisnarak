package br.edu.unifor.uniforfit.gestor

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.Exercicio

/** Editar Exercício (RF38) e cadastro de novo exercício (RF37.4). */
class EditarExercicioActivity : FormularioActivity() {

    private var exercicio: Exercicio? = null
    private var midiaEscolhida = false

    private lateinit var etNome: EditText
    private lateinit var etMaquina: EditText
    private lateinit var etInstrucoes: EditText
    private lateinit var spGrupo: Spinner
    private lateinit var spSetor: Spinner
    private lateinit var btnSalvar: TextView
    private lateinit var imgMidia: ImageView

    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        foto?.let {
            imgMidia.setImageBitmap(it)
            mostrarMidia()
        }
    }
    private val galeria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imgMidia.setImageURI(it)
            mostrarMidia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_exercicio)

        val id = intent.getIntExtra(EXTRA_ID, -1)
        exercicio = Dados.exercicios.firstOrNull { it.id == id }
        etNome = findViewById(R.id.etNomeExercicio)
        etMaquina = findViewById(R.id.etMaquina)
        etInstrucoes = findViewById(R.id.etInstrucoes)
        spGrupo = findViewById(R.id.spGrupo)
        spSetor = findViewById(R.id.spSetor)
        btnSalvar = findViewById(R.id.btnSalvarExercicio)
        imgMidia = findViewById(R.id.imgMidia)

        val atual = exercicio
        if (atual == null) findViewById<TextView>(R.id.tvTitulo).text = "Novo Exercício"
        if (savedInstanceState == null) {
            etNome.setText(atual?.nome)
            etMaquina.setText(atual?.maquina)
            etInstrucoes.setText(atual?.instrucoes)
        }
        spGrupo.preencher(Dados.opcoesGrupo, atual?.grupo)
        spSetor.preencher(Dados.opcoesSetor, atual?.setor)
        marcarComoSalvo()

        listOf(etNome, etMaquina, etInstrucoes).forEach { it.doAfterTextChanged { atualizarBotao() } }
        atualizarBotao()

        // RF38.5 — câmera ou galeria
        findViewById<View>(R.id.btnAdicionarMidia).setOnClickListener {
            Popup.escolherMidia(
                this,
                titulo = "Foto/vídeo do exercício",
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
        btnSalvar.setOnClickListener { salvar() }
    }

    override fun estadoAtual() = listOf(
        etNome.text.toString(), etMaquina.text.toString(), etInstrucoes.text.toString(),
        spGrupo.selectedItemPosition, spSetor.selectedItemPosition, midiaEscolhida,
    )

    private fun mostrarMidia() {
        midiaEscolhida = true
        imgMidia.isVisible = true
        imgMidia.clipToOutline = true
    }

    /** RF38.6 — "Salvar exercício" só habilita com os obrigatórios preenchidos. */
    private fun atualizarBotao() {
        val completo = listOf(etNome, etMaquina, etInstrucoes).all { it.text.isNotBlank() }
        btnSalvar.isEnabled = completo
        btnSalvar.alpha = if (completo) 1f else 0.4f
    }

    private fun salvar() {
        val atual = exercicio
        val nome = etNome.text.toString().trim()
        val maquina = etMaquina.text.toString().trim()
        val instrucoes = etInstrucoes.text.toString().trim()
        if (atual == null) {
            Dados.exercicios.add(Exercicio(Dados.proximoIdExercicio(), nome, maquina, spGrupo.valor, spSetor.valor, instrucoes))
        } else {
            atual.nome = nome
            atual.maquina = maquina
            atual.grupo = spGrupo.valor
            atual.setor = spSetor.valor
            atual.instrucoes = instrucoes // aparece para o aluno em "Como executar" (RF38.4)
        }
        voltarPara(ExerciciosActivity::class.java) {
            putExtra(EXTRA_AVISO, if (atual == null) "Exercício adicionado ao catálogo!" else "Exercício \"$nome\" salvo.")
        }
    }

    companion object {
        const val EXTRA_ID = "id_exercicio"
    }
}
