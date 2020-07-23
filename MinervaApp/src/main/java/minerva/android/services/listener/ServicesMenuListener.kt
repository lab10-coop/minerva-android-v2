package minerva.android.services.listener

import minerva.android.walletmanager.storage.ServiceType

interface ServicesMenuListener {
    fun onRemoved(@ServiceType type: String, name:String)
}