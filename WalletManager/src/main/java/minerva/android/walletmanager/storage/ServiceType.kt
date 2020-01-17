package minerva.android.walletmanager.storage

import androidx.annotation.IntDef
import minerva.android.walletmanager.storage.ServiceType.Companion.MINERVA

@Retention(AnnotationRetention.SOURCE)
@IntDef(MINERVA)
annotation class ServiceType {
    companion object {
        const val MINERVA = 0
    }
}