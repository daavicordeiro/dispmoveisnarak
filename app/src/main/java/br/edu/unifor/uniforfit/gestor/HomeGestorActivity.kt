package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.data.Dados

/** Painel Inicial do Gestor (RF28). */
class HomeGestorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_gestor)

        // RF28.3 — atalhos da seção "Gerenciar"
        findViewById<View>(R.id.cardAlunos).setOnClickListener { abrir(AlunosActivity::class.java) }
        findViewById<View>(R.id.cardTreinos).setOnClickListener { abrir(TreinosListaActivity::class.java) }
        findViewById<View>(R.id.cardExercicios).setOnClickListener { abrir(ExerciciosActivity::class.java) }
        findViewById<View>(R.id.cardLembretes).setOnClickListener { abrir(LembretesActivity::class.java) }
        findViewById<View>(R.id.btnPerfilGestor).setOnClickListener { abrir(PerfilGestorActivity::class.java) }
    }

    /** RF28.1 / RF28.2 — os totais mudam conforme o gestor cadastra ou exclui itens. */
    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.tvNomeGestor).text = Dados.nomeGestor
        findViewById<TextView>(R.id.tvAlunosAtivos).text = Dados.alunos.count { it.status == "Ativo" }.toString()
        findViewById<TextView>(R.id.tvTreinosCadastrados).text = Dados.treinos.size.toString()
        findViewById<TextView>(R.id.tvAnamnesesPendentes).text = Dados.alunos.count { it.status == "Pendente" }.toString()
        findViewById<TextView>(R.id.tvLembretesAtivos).text = Dados.lembretes.count { it.ativo }.toString()
        findViewById<TextView>(R.id.tvResumoAlunos).text = "${Dados.alunos.size} cadastrados"
        findViewById<TextView>(R.id.tvResumoTreinos).text = "${Dados.treinos.size} modelos ativos"
        findViewById<TextView>(R.id.tvResumoExercicios).text = "${Dados.exercicios.size} no catálogo"
        findViewById<TextView>(R.id.tvResumoLembretes).text = "${Dados.lembretes.size} programados"
    }
}
