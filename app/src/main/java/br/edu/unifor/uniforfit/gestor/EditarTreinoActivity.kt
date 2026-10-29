package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.ItemTreino
import br.edu.unifor.uniforfit.data.Treino

/**
 * Editar Treino (RF36) e cadastro de novo treino (RF35.3).
 * As mudanças ficam numa cópia até "Salvar treino", que as propaga aos alunos (RF36.5).
 */
class EditarTreinoActivity : FormularioActivity() {

    private var treino: Treino? = null
    private val itens = mutableListOf<ItemTreino>()

    private lateinit var etNome: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var spNivel: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_treino)

        treino = Dados.treino(intent.getIntExtra(EXTRA_ID, -1))
        etNome = findViewById(R.id.etNomeTreino)
        spCategoria = findViewById(R.id.spCategoria)
        spNivel = findViewById(R.id.spNivel)

        val atual = treino
        if (atual == null) findViewById<TextView>(R.id.tvTitulo).text = "Novo Treino"
        // RF36 — dados carregados automaticamente
        if (savedInstanceState == null) etNome.setText(atual?.nome)
        spCategoria.preencher(Dados.opcoesCategoria, atual?.categoria)
        spNivel.preencher(Dados.opcoesNivel, atual?.nivel)
        atual?.itens?.forEach { itens.add(it.copy()) }
        montarItens()
        marcarComoSalvo()

        findViewById<View>(R.id.btnAdicionarExercicio).setOnClickListener { escolherExercicios() }
        findViewById<View>(R.id.btnSalvarTreino).setOnClickListener { salvar() }
    }

    override fun estadoAtual() = listOf(
        etNome.text.toString(), spCategoria.selectedItemPosition, spNivel.selectedItemPosition,
        itens.map { it.exercicio.id },
    )

    /** RF36.2 — exercícios com a prescrição; RF36.3 — "X" para remover. */
    private fun montarItens() {
        val lista = findViewById<LinearLayout>(R.id.listaExerciciosTreino)
        lista.removeAllViews()
        itens.forEachIndexed { i, item ->
            val linha = layoutInflater.inflate(R.layout.item_exercicio_editar, lista, false)
            linha.findViewById<TextView>(R.id.tvNome).text = item.exercicio.nome
            linha.findViewById<TextView>(R.id.tvPrescricao).text = item.prescricaoCurta
            linha.findViewById<View>(R.id.btnRemover).apply {
                contentDescription = "Remover ${item.exercicio.nome}"
                setOnClickListener { confirmarRemocao(i) }
            }
            lista.addView(linha)
        }
        findViewById<View>(R.id.tvSemExercicios).isVisible = itens.isEmpty()
    }

    private fun confirmarRemocao(indice: Int) {
        val item = itens.getOrNull(indice) ?: return
        Popup.mostrar(
            this,
            titulo = "Remover exercício?",
            mensagem = "${item.exercicio.nome} sai deste treino, mas continua no catálogo de exercícios.",
            tipo = Popup.Tipo.PERIGO,
            botao = "Remover",
            botaoSecundario = "Cancelar",
        ) {
            itens.removeAt(indice)
            montarItens()
        }
    }

    /** RF36.4 — escolha no catálogo, em pop-up. */
    private fun escolherExercicios() {
        val disponiveis = Dados.exercicios.filter { exercicio -> itens.none { it.exercicio.id == exercicio.id } }
        if (disponiveis.isEmpty()) {
            avisar("Todos os exercícios do catálogo já estão neste treino.")
            return
        }
        val marcacoes = MarcacoesPopup(this, disponiveis.map { "${it.nome}  ·  ${it.grupo}" })
        Popup.mostrar(
            this,
            titulo = "Adicionar exercícios",
            mensagem = "Escolha no catálogo da academia. A prescrição começa em 3×12 e pode ser ajustada pelo aluno.",
            tipo = null,
            botao = "Adicionar",
            botaoSecundario = "Cancelar",
            conteudo = marcacoes.view,
            validar = { marcacoes.marcadas.any { it } },
        ) {
            val escolhidos = disponiveis.filterIndexed { i, _ -> marcacoes.marcadas[i] }
            escolhidos.forEach { exercicio ->
                itens.add(ItemTreino(exercicio, 3, 12, if (exercicio.grupo == "Cardio") 0 else 20))
            }
            montarItens()
            avisar(if (escolhidos.size == 1) "1 exercício adicionado." else "${escolhidos.size} exercícios adicionados.")
        }
    }

    private fun salvar() {
        val nome = etNome.text.toString().trim()
        if (nome.length < 3) {
            etNome.error = "Dê um nome ao treino"
            findViewById<View>(R.id.btnSalvarTreino).tremer()
            return
        }
        if (itens.isEmpty()) {
            avisar("Adicione pelo menos um exercício ao treino.")
            findViewById<View>(R.id.btnAdicionarExercicio).tremer()
            return
        }

        val atual = treino
        val gravar = {
            if (atual == null) {
                Dados.treinos.add(Treino(
                    Dados.proximoIdTreino(), nome, spCategoria.valor, spNivel.valor,
                    duracaoMin = itens.size * 12, alunos = 0, itens = itens.toMutableList(),
                ))
            } else {
                Dados.alunos.filter { it.treino == atual.nome }.forEach { it.treino = nome }
                atual.nome = nome
                atual.categoria = spCategoria.valor
                atual.nivel = spNivel.valor
                atual.itens.clear()
                atual.itens.addAll(itens)
            }
            voltarPara(TreinosListaActivity::class.java) {
                putExtra(EXTRA_AVISO, if (atual == null) "Treino criado!" else "Treino salvo e enviado aos alunos.")
            }
        }

        // RF36.5 — avisa que a alteração chega na hora para quem usa o treino
        if (atual != null && atual.alunos > 0) {
            Popup.mostrar(
                this,
                titulo = "Salvar e enviar aos alunos?",
                mensagem = "As mudanças em \"$nome\" chegam imediatamente ao app dos ${atual.alunos} alunos que usam este treino.",
                tipo = Popup.Tipo.PERGUNTA,
                botao = "Salvar e enviar",
                botaoSecundario = "Cancelar",
            ) { gravar() }
        } else {
            gravar()
        }
    }

    companion object {
        const val EXTRA_ID = "id_treino"
    }
}
