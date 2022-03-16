package minerva.android.configProvider.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics

class LoggerImpl : Logger {
    override fun logVersion(publicKey: String, version: Int) {
        FirebaseCrashlytics.getInstance()
            .recordException(Throwable("PublicKey: $publicKey saveWalletConfig($version)"))
    }
}