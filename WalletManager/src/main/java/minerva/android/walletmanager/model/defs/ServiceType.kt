package minerva.android.walletmanager.model.defs

import androidx.annotation.IntDef
import minerva.android.walletmanager.model.defs.ServiceType.Companion.M27
import minerva.android.walletmanager.model.defs.ServiceType.Companion.ÖAMTC_FUEL_SERVICE

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@IntDef(M27, ÖAMTC_FUEL_SERVICE)
annotation class ServiceType {
    companion object {
        const val M27 = 1
        const val ÖAMTC_FUEL_SERVICE = 2
    }
}