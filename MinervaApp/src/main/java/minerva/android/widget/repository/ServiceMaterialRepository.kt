package minerva.android.widget.repository

import minerva.android.R
import minerva.android.walletmanager.model.defs.ServiceType.Companion.CHARGING_STATION
import minerva.android.walletmanager.model.defs.ServiceType.Companion.M27
import minerva.android.walletmanager.model.defs.ServiceType.Companion.UNICORN_LOGIN

fun getServiceIcon(type: String): Int =
    when (type) {
        M27 -> R.mipmap.ic_m27
        CHARGING_STATION -> R.drawable.ic_charging_station
        UNICORN_LOGIN -> R.mipmap.ic_unicorn
        else -> R.mipmap.ic_minerva
    }