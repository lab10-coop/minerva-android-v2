package minerva.android.walletmanager.repository.walletconnect

import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

sealed class WalletConnectStatus
data class OnSessionRequest(val meta: WalletConnectPeerMeta, val chainId: Int?, val topic: Topic) :
    WalletConnectStatus()

object OnDisconnect : WalletConnectStatus()
data class OnEthSign(val message: String, val peerId: String) : WalletConnectStatus()
data class OnEthSendTransaction(val transaction: WalletConnectTransaction, val peerId: String) : WalletConnectStatus()