package minerva.android.walletConnect.repository

interface WalletConnectRepository {
    fun connect(qrCode: String)
    fun approve()
    fun close()
}