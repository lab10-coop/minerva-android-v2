package minerva.android.walletConnect.repository

import io.reactivex.Flowable
import minerva.android.walletConnect.client.WalletConnectStatus

interface WalletConnectRepository {
    fun connect(qrCode: String)
    fun approveSession(addresses: List<String>, chainId: Int)
    fun rejectSession()
    fun killSession()
    fun disconnect()
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
}