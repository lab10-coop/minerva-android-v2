package minerva.android.services.listener

import minerva.android.walletmanager.model.defs.ServiceType

interface ServicesMenuListener {
    fun onRemoved(@ServiceType type: String, name:String)
}