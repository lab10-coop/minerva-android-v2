package minerva.android.walletConnect.storage

import minerva.android.walletConnect.model.session.Dapp

interface WalletConnectStorage {
    fun saveDapp(address: String, dapp: Dapp)
    fun getConnectedDapps(address: String): List<Dapp>
    fun removeDapp(address: String, dapp: Dapp)
    fun clear()
}