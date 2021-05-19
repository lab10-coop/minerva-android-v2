package minerva.android.extensions

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import minerva.android.R
import timber.log.Timber

fun Fragment.showBiometricPrompt(
    onSuccessAction: () -> Unit,
    onCancelledAction: () -> Unit = {},
    notSystemAuthorizationAction: () -> Unit = onSuccessAction
) {
    activity?.let {
        (it.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).let { keyguard ->
            if (keyguard.isDeviceSecure) showBiometricPrompt({ onSuccessAction() }, { onCancelledAction() })
            else notSystemAuthorizationAction()
        }
    }
}

private fun Fragment.showBiometricPrompt(onSuccessAction: () -> Unit, onCancelledAction: () -> Unit = {}) {
    val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.e("Authentication error: $errString")
                onCancelledAction()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccessAction()
            }
        })

    BiometricPrompt.PromptInfo.Builder().setTitle(context?.getString(R.string.authentication_title).toString()).apply {
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK.or(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) setAllowedAuthenticators(allowedAuthenticators)
        else setDeviceCredentialAllowed(true)
        biometricPrompt.authenticate(build())
    }
}