package minerva.android.widget.dialog

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.widget.MinervaFlashbar
import timber.log.Timber

object BiometricDialog {
    fun show(fragment: Fragment, onSuccessAction: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(fragment.context)
        val biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccessAction()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(fragment.context?.getString(R.string.authentication_title).toString())
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}