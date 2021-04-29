package minerva.android.extensions

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.widget.MinervaFlashbar
import timber.log.Timber

fun Fragment.showBiometricPrompt(onSuccessAction: () -> Unit, onFailAction: () -> Unit = onSuccessAction) {
    activity?.let {
        (it.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).let { keyguard ->
            if (keyguard.isDeviceSecure) showBiometricPrompt(this) { onSuccessAction() }
            else onFailAction()
        }
    }
}

private fun showBiometricPrompt(fragment: Fragment, onSuccessAction: () -> Unit) {
    fragment.run {
        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
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

        BiometricPrompt.PromptInfo.Builder().setTitle(context?.getString(R.string.authentication_title).toString()).apply {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            else setDeviceCredentialAllowed(true)
            biometricPrompt.authenticate(build())
        }
    }
}

