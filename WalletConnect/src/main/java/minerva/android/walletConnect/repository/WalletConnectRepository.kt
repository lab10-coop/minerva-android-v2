package minerva.android.walletConnect.repository

import io.reactivex.Flowable
import minerva.android.walletConnect.client.WalletConnectStatus

interface WalletConnectRepository {
    fun connect(qrCode: String)
    fun approve()
    fun close()
    val connectionStatusFlowable: Flowable<WalletConnectStatus>
}