package minerva.android.walletConnect.repository

import minerva.android.walletConnect.model.session.WCSession

interface WalletConnectRepository {
    fun getWCSession(qrCode: String): WCSession?
    fun setupClient()
    fun connect()
    fun approve()
}