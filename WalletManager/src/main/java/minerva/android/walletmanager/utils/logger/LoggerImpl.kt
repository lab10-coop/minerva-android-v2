package minerva.android.walletmanager.utils.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics

class LoggerImpl : Logger {
    override fun logToFirebase(message: String) {
        FirebaseCrashlytics.getInstance().recordException(Throwable(message))
    }
}