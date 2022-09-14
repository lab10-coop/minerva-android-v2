package minerva.android.walletmanager.utils.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics
import minerva.android.walletmanager.manager.wallet.WalletConfigManager

class LoggerImpl(private val walletConfigManager: WalletConfigManager) : Logger {

    override fun logToFirebase(message: String) {
        if (::walletConfigManager.masterSeed.isInitialized) {
            FirebaseCrashlytics.getInstance()
                .recordException(Throwable("PublicKey: ${walletConfigManager.masterSeed.publicKey} $message"))
        } else {
            FirebaseCrashlytics.getInstance()
                .recordException(Throwable(message))
        }
    }
}