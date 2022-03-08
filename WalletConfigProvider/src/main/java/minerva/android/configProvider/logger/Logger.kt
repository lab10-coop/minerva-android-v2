package minerva.android.configProvider.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics

interface Logger {
    fun logVersion(publicKey: String, version: Int)
}