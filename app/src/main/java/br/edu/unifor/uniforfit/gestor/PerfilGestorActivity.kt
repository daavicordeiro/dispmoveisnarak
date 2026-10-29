package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.ui.perfil.AcessibilidadeActivity

/** Perfil do Gestor (RF40). */
class PerfilGestorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_gestor)

        findViewById<View>(R.id.itemMeusDados).setOnClickListener { editarMeusDados() } // RF40.1
        findViewById<View>(R.id.itemAlunosAtribuidos).setOnClickListener { // RF40.2
            abrir(AlunosActivity::class.java) { putExtra(AlunosActivity.EXTRA_SOMENTE_MEUS, true) }
        }
        findViewById<View>(R.id.itemNotificacoesPainel).setOnClickListener { configurarAvisos() } // RF40.3
        findViewById<View>(R.id.itemNomenclatura).setOnClickListener { // RF40.4
            abrir(ExerciciosActivity::class.java)
        }
        findViewById<View>(R.id.itemAcessibilidade).setOnClickListener { // RF40.6
            abrir(AcessibilidadeActivity::class.java) { putExtra(AcessibilidadeActivity.EXTRA_GESTOR, true) }
        }
        findViewById<View>(R.id.itemSair).setOnClickListener { // RF40.5
            Popup.mostrar(
                this,
                titulo = getString(R.string.uf_sair_confirmar_titulo),
                mensagem = "Você voltará para a tela de login do Painel do Gestor.",
                tipo = Popup.Tipo.PERIGO,
                botao = getString(R.string.uf_sair),
                botaoSecundario = getString(R.string.uf_cancelar),
            ) { abrirComoInicio(LoginGestorActivity::class.java) }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.tvNomeGestor).text = Dados.nomeGestor
        findViewById<TextView>(R.id.tvEmailGestor).text = "Educador físico · Unifor Fit"
    }

    private fun editarMeusDados() {
        val campos = CamposPopup(this).configurar(
            rotulo1 = "Nome", valor1 = Dados.nomeGestor,
            rotulo2 = "E-mail institucional", valor2 = Dados.emailGestor,
        )
        campos.campo1.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        campos.campo2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        Popup.mostrar(
            this,
            titulo = "Meus dados",
            tipo = null,
            botao = "Salvar",
            botaoSecundario = "Cancelar",
            conteudo = campos.view,
            validar = {
                val nomeOk = campos.campo1.text.trim().length >= 3
                val emailOk = campos.campo2.text.contains("@")
                if (!nomeOk) campos.campo1.error = "Informe seu nome"
                if (!emailOk) campos.campo2.error = "E-mail inválido"
                nomeOk && emailOk
            },
        ) {
            Dados.nomeGestor = campos.campo1.text.toString().trim()
            Dados.emailGestor = campos.campo2.text.toString().trim()
            findViewById<TextView>(R.id.tvNomeGestor).text = Dados.nomeGestor
            avisar("Dados atualizados.")
        }
    }

    private fun configurarAvisos() {
        val opcoes = Dados.avisosPainel.keys.toList()
        val marcacoes = MarcacoesPopup(this, opcoes, opcoes.map { Dados.avisosPainel.getValue(it) })
        Popup.mostrar(
            this,
            titulo = "Notificações do painel",
            mensagem = "Escolha quais avisos você quer receber.",
            tipo = null,
            botao = "Salvar",
            botaoSecundario = "Cancelar",
            conteudo = marcacoes.view,
        ) {
            opcoes.forEachIndexed { i, aviso -> Dados.avisosPainel[aviso] = marcacoes.marcadas[i] }
            avisar("Preferências de notificação salvas.")
        }
    }
}
