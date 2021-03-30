package minerva.android.widget.dialog

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object BiometricDialog {
    fun show(fragment: Fragment, onFailMessage: String, onErrorMessage: String, onSuccessAction: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(fragment.context)
        val biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("klop", "Biometric dialog E R R O R !")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.e("klop", "Biometric dialog S U C C E S S !")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e("klop", "Biometric F A I L !")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}