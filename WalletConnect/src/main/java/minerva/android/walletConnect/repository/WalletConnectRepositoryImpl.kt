package minerva.android.walletConnect.repository

import minerva.android.walletConnect.model.session.WCSession

class WalletConnectRepositoryImpl() : WalletConnectRepository {

    override fun getWCSession(qrCode: String): WCSession? = WCSession.from(qrCode)

    override fun setupClient() {
        TODO("set wss listeners")
    }

    override fun connect() {
        TODO("connect to wss, handshake")
    }

    override fun approve() {
        TODO("approve connection")
    }

}