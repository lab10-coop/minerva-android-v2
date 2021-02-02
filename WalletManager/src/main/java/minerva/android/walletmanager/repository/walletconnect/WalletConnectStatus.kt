package minerva.android.walletmanager.repository.walletconnect

import minerva.android.walletmanager.model.Topic
import minerva.android.walletmanager.model.WalletConnectPeerMeta

sealed class WalletConnectStatus
data class OnSessionRequest(val meta: WalletConnectPeerMeta, val chainId: Int?, val topic: Topic) :
    WalletConnectStatus()

data class OnConnectionFailure(val error: Throwable, val peerId: String?) : WalletConnectStatus()
data class OnDisconnect(val reason: Int, val peerId: String?) : WalletConnectStatus()