package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados

/** Alunos (RF29). Com [EXTRA_SOMENTE_MEUS] vira "Alunos atribuídos a mim" (RF40.2). */
class AlunosActivity : BaseActivity() {

    private val somenteMeus by lazy { intent.getBooleanExtra(EXTRA_SOMENTE_MEUS, false) }
    private lateinit var etBuscar: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alunos)

        if (somenteMeus) {
            findViewById<TextView>(R.id.tvTitulo).text = "Alunos atribuídos a mim"
            findViewById<TextView>(R.id.tvVoltar).text = "← Perfil"
        }
        etBuscar = findViewById(R.id.etBuscar)
        etBuscar.doAfterTextChanged { montarLista() } // RF29.1
        // RF29.5
        findViewById<View>(R.id.btnNovoAluno).setOnClickListener { abrir(EditarAlunoActivity::class.java) }
    }

    override fun onResume() {
        super.onResume()
        montarLista()
    }

    private fun montarLista() {
        val busca = etBuscar.text.toString().trim().lowercase()
        val alunos = Dados.alunos.filter { aluno ->
            (!somenteMeus || aluno.doProfessorLogado) &&
                (busca.isEmpty() || aluno.nome.lowercase().contains(busca) || aluno.ra.contains(busca))
        }
        val lista = findViewById<LinearLayout>(R.id.listaAlunos)
        lista.removeAllViews()
        alunos.forEach { aluno ->
            val card = layoutInflater.inflate(R.layout.item_aluno, lista, false)
            card.findViewById<TextView>(R.id.tvNome).text = aluno.nome
            card.findViewById<TextView>(R.id.tvRa).text = "R.A. ${aluno.ra}"
            // RF29.3 — cores distintas por status
            card.findViewById<TextView>(R.id.tvStatus).apply {
                text = aluno.status
                backgroundTintList = ContextCompat.getColorStateList(context, corDoStatus(aluno.status))
            }
            // RF29.4
            card.setOnClickListener {
                abrir(FichaAlunoActivity::class.java) { putExtra(EXTRA_RA, aluno.ra) }
            }
            lista.addView(card)
        }
        findViewById<View>(R.id.tvSemResultados).isVisible = alunos.isEmpty()
    }

    companion object {
        const val EXTRA_SOMENTE_MEUS = "somente_meus"
        const val EXTRA_RA = "ra"

        fun corDoStatus(status: String) = when (status) {
            "Ativo" -> R.color.verde
            "Pendente" -> R.color.vermelho
            else -> R.color.texto_cinza
        }
    }
}
