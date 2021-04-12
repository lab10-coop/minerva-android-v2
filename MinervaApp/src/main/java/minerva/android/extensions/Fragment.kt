package minerva.android.extensions

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import minerva.android.R
import timber.log.Timber

fun Fragment.showBiometricPrompt(onSuccessAction: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(this, executor,
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
        .setTitle(context?.getString(R.string.authentication_title).toString())
        .setDeviceCredentialAllowed(true)
        .build()
    biometricPrompt.authenticate(promptInfo)
}