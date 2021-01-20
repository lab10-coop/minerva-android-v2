package minerva.android.walletConnect.repository

import io.reactivex.Flowable
import minerva.android.walletConnect.client.WalletConnectStatus

interface WalletConnectRepository {
    fun connect(qrCode: String)
    fun approveSession(addresses: List<String>, chainId: Int, peerId: String)
    fun rejectSession(peerId: String)
    fun killSession(peerId: String)
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
}