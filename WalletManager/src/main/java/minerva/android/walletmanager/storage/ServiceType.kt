package minerva.android.walletmanager.storage

import androidx.annotation.StringDef
import minerva.android.walletmanager.storage.ServiceType.Companion.CHARGING_STATION
import minerva.android.walletmanager.storage.ServiceType.Companion.M27
import minerva.android.walletmanager.storage.ServiceType.Companion.UNICORN_LOGIN

@Retention(AnnotationRetention.SOURCE)
@StringDef(UNICORN_LOGIN, M27, CHARGING_STATION)
annotation class ServiceType {
    companion object {
        const val UNICORN_LOGIN = "did:ethr:0x95b200870916377a74fc65d628a735d58bc22c98"
        const val M27 = "1"
        const val CHARGING_STATION = "did:ethr:0x39fc0b7040cab1df11aba6a532ae7f429d2a7f75"
    }
}