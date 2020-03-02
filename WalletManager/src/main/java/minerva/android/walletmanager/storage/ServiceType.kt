package minerva.android.walletmanager.storage

import androidx.annotation.IntDef
import minerva.android.walletmanager.storage.ServiceType.Companion.M27
import minerva.android.walletmanager.storage.ServiceType.Companion.DEMO_LOGIN

@Retention(AnnotationRetention.SOURCE)
@IntDef(DEMO_LOGIN, M27)
annotation class ServiceType {
    companion object {
        const val DEMO_LOGIN = 0
        const val M27 = 1
    }
}