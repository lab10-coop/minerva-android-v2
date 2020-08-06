package minerva.android.walletmanager.model.defs

import androidx.annotation.StringDef
import minerva.android.walletmanager.model.defs.ServiceName.Companion.CHARGING_STATION_NAME
import minerva.android.walletmanager.model.defs.ServiceName.Companion.M27_NAME
import minerva.android.walletmanager.model.defs.ServiceName.Companion.UNICORN_LOGIN_NAME

@Retention(AnnotationRetention.SOURCE)
@StringDef(UNICORN_LOGIN_NAME, CHARGING_STATION_NAME, M27_NAME)
annotation class ServiceName {
    companion object {
        const val UNICORN_LOGIN_NAME = "Demo Web Page Login"
        const val M27_NAME = "M27"
        const val CHARGING_STATION_NAME = "Charging Station Dashboard"
    }
}