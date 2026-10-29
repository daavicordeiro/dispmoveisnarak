package br.edu.unifor.uniforfit.ui.comum

import android.os.Bundle
import android.widget.TextView
import br.edu.unifor.uniforfit.R
import br.edu.unifor.uniforfit.core.BaseActivity
import br.edu.unifor.uniforfit.core.Rede

/**
 * Erro — Sem Conexão (RF23). Aberta pela BaseActivity.exigirConexao; quando volta com RESULT_OK,
 * a tela anterior repete a última ação automaticamente (RF23.1).
 */
class SemConexaoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setConteudoComAssistente(R.layout.activity_sem_conexao)

        val btnTentar = findViewById<TextView>(R.id.btnTentarNovamente)
        btnTentar.setOnClickListener {
            if (Rede.estaOnline(this)) {
                setResult(RESULT_OK)
                finish()
            } else {
                btnTentar.tremer()
                avisar("Ainda sem conexão. Verifique o Wi-Fi ou os dados móveis.")
            }
        }
    }
}
