package minerva.android.services.login.uitls

import androidx.annotation.IntDef
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER

@Retention(AnnotationRetention.SOURCE)
@IntDef(NEW_USER, KNOWN_USER, NEW_QUICK_USER, KNOWN_QUICK_USER)
annotation class LoginStatus {
    companion object {
        const val NEW_USER = 0
        const val KNOWN_USER = 1
        const val NEW_QUICK_USER = 2
        const val KNOWN_QUICK_USER = 3
    }
}