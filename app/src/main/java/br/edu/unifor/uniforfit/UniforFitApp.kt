package br.edu.unifor.uniforfit

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Inicializa o Firebase. O App Check é exigido pelo Firebase AI Logic (assistente com Gemini).
 * Em desenvolvimento usa o provedor de debug: o token aparece no Logcat
 * ("Enter this debug secret into the allow list") e deve ser cadastrado no console,
 * em App Check → Apps → Gerenciar tokens de depuração.
 */
class UniforFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
