package minerva.android.widget.repository

import minerva.android.R
import minerva.android.walletmanager.storage.ServiceType

fun getServiceIcon(type: String): Int =
    when (type) {
        ServiceType.M27 -> R.mipmap.ic_m27
        ServiceType.CHARGING_STATION -> R.drawable.ic_charging_station
        else -> R.mipmap.ic_unicorn
    }