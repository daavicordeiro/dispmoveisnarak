package br.edu.unifor.uniforfit.gestor

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Popup
import br.edu.unifor.uniforfit.data.Dados
import br.edu.unifor.uniforfit.data.Lembrete
import com.google.android.material.materialswitch.MaterialSwitch

/** Lembretes (RF39) — regras de notificação automática enviadas aos alunos. */
class LembretesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lembretes)
        findViewById<View>(R.id.btnNovoLembrete).setOnClickListener { novoLembrete() }
        montarLista()
    }

    private fun montarLista() {
        val lista = findViewById<LinearLayout>(R.id.listaLembretes)
        lista.removeAllViews()
        Dados.lembretes.forEach { lembrete ->
            val linha = layoutInflater.inflate(R.layout.item_lembrete, lista, false)
            linha.findViewById<TextView>(R.id.tvNome).text = lembrete.nome
            linha.findViewById<TextView>(R.id.tvCondicao).text = lembrete.condicao
            // RF39.2 — liga/desliga com efeito imediato
            linha.findViewById<MaterialSwitch>(R.id.swAtivo).apply {
                isChecked = lembrete.ativo
                contentDescription = lembrete.nome
                setOnCheckedChangeListener { _, ativo ->
                    lembrete.ativo = ativo
                    avisar(if (ativo) "\"${lembrete.nome}\" ativado." else "\"${lembrete.nome}\" desativado.")
                }
            }
            lista.addView(linha)
        }
    }

    /** RF39.4 — nova regra criada num pop-up com formulário. */
    private fun novoLembrete() {
        val campos = CamposPopup(this).configurar(
            rotulo1 = "Nome da regra", dica1 = "Ex: Parabéns pelo recorde",
            rotulo2 = "Condição de disparo", dica2 = "Ex: Após 5 dias sem check-in",
        )
        Popup.mostrar(
            this,
            titulo = "Nova regra de lembrete",
            tipo = null,
            botao = "Criar lembrete",
            botaoSecundario = "Cancelar",
            conteudo = campos.view,
            validar = {
                val nomeOk = campos.campo1.text.isNotBlank()
                val condicaoOk = campos.campo2.text.isNotBlank()
                if (!nomeOk) campos.campo1.error = "Obrigatório"
                if (!condicaoOk) campos.campo2.error = "Obrigatório"
                nomeOk && condicaoOk
            },
        ) {
            Dados.lembretes.add(Lembrete(campos.campo1.text.toString().trim(), campos.campo2.text.toString().trim(), true))
            montarLista()
            avisar("Lembrete criado e ativado.")
        }
    }
}
