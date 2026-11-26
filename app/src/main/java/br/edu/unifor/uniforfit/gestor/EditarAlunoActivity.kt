package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Aluno
import br.edu.unifor.uniforfit.data.Banco
import br.edu.unifor.uniforfit.data.Dados
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Editar Aluno (RF30) e cadastro de novo aluno (RF29.5). */
class EditarAlunoActivity : FormularioActivity() {

    private var aluno: Aluno? = null

    private lateinit var etNome: EditText
    private lateinit var etRa: EditText
    private lateinit var etEmail: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spTurma: Spinner
    private lateinit var spTreino: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_aluno)

        aluno = intent.getStringExtra(AlunosActivity.EXTRA_RA)?.let { ra -> Dados.alunos.firstOrNull { it.ra == ra } }
        etNome = findViewById(R.id.etNome)
        etRa = findViewById(R.id.etRa)
        etEmail = findViewById(R.id.etEmail)
        spStatus = findViewById(R.id.spStatus)
        spTurma = findViewById(R.id.spTurma)
        spTreino = findViewById(R.id.spTreino)

        val atual = aluno
        if (atual == null) {
            findViewById<TextView>(R.id.tvTitulo).text = "Novo Aluno"
            findViewById<TextView>(R.id.btnExcluir).isVisible = false
        }
        // RF30 — dados carregados automaticamente
        if (savedInstanceState == null) {
            etNome.setText(atual?.nome)
            etRa.setText(atual?.ra)
            etEmail.setText(atual?.email)
        }
        spStatus.preencher(Dados.opcoesStatus, atual?.status ?: "Pendente")
        spTurma.preencher(Dados.opcoesTurma, atual?.turma)
        spTreino.preencher(Dados.treinos.map { it.nome }, Dados.treino(atual?.treinoId)?.nome)
        marcarComoSalvo()

        findViewById<TextView>(R.id.btnSalvar).setOnClickListener { salvar() }
        findViewById<TextView>(R.id.btnExcluir).setOnClickListener { confirmarExclusao() }
    }

    override fun estadoAtual() = listOf(
        etNome.text.toString(), etRa.text.toString(), etEmail.text.toString(),
        spStatus.selectedItemPosition, spTurma.selectedItemPosition, spTreino.selectedItemPosition,
    )

    /** RF30.3 — valida os obrigatórios; RF30.4 — salva e volta à listagem. */
    private fun salvar() {
        val nome = etNome.text.toString().trim()
        val ra = etRa.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val raEmUso = Dados.alunos.any { it.ra == ra && it !== aluno }

        var valido = true
        if (nome.length < 3) {
            etNome.error = "Informe o nome completo"
            valido = false
        }
        if (ra.length != 10 || raEmUso) {
            etRa.error = if (raEmUso) "Este R.A. já pertence a outro aluno" else "O R.A. tem 10 dígitos"
            valido = false
        }
        if (!email.contains("@") || !email.contains(".")) {
            etEmail.error = "E-mail inválido"
            valido = false
        }
        if (!valido) {
            findViewById<TextView>(R.id.btnSalvar).tremer()
            return
        }

        val atual = aluno
        val treinoId = Dados.treinos.getOrNull(spTreino.selectedItemPosition)?.id
        if (atual == null) {
            val mes = SimpleDateFormat("MMM/yyyy", Locale.forLanguageTag("pt-BR")).format(Date()).replace(".", "")
            val novo = Aluno(nome, ra, email, spStatus.valor, spTurma.valor, treinoId, mes, Dados.gestorUid,
                onboardingPendente = true, plano = planoPara(treinoId))
            Dados.alunos.add(novo)
            Banco.salvarAluno(novo, raAntigo = null)
        } else {
            val raAntigo = atual.ra
            if (atual.treinoId != treinoId) atual.plano = planoPara(treinoId)
            atual.nome = nome
            atual.ra = ra
            atual.email = email
            atual.status = spStatus.valor
            atual.turma = spTurma.valor
            atual.treinoId = treinoId
            Banco.salvarAluno(atual, raAntigo)
        }
        voltarPara(AlunosActivity::class.java) {
            putExtra(EXTRA_AVISO, if (atual == null) "Aluno cadastrado!" else "Dados de $nome salvos.")
        }
    }

    /** O treino atribuído vira o plano da semana: segunda, quarta e sexta, com descanso entre eles. */
    private fun planoPara(treinoId: Int?): MutableList<String> {
        val t = treinoId?.let { "t:$it" } ?: ""
        return mutableListOf(t, "descanso", t, "descanso", t, "", "")
    }

    /** RF30.5 — exclusão sempre confirmada. */
    private fun confirmarExclusao() {
        val atual = aluno ?: return
        Popup.mostrar(
            this,
            titulo = "Excluir aluno?",
            mensagem = "${atual.nome} perderá o acesso ao app e os dados clínicos serão apagados. " +
                "Essa ação não pode ser desfeita.",
            tipo = Popup.Tipo.PERIGO,
            botao = "Excluir aluno",
            botaoSecundario = "Cancelar",
        ) {
            Banco.excluirAluno(atual)
            voltarPara(AlunosActivity::class.java) { putExtra(EXTRA_AVISO, "${atual.nome} foi excluído(a).") }
        }
    }
}
