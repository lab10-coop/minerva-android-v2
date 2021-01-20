package minerva.android.walletConnect.client

import minerva.android.walletConnect.model.session.WCPeerMeta

sealed class WalletConnectStatus
data class OnSessionRequest(val meta: WCPeerMeta, val chainId: Int?, val peerId: String?) :
    WalletConnectStatus()

data class OnConnectionFailure(val error: Throwable) : WalletConnectStatus()
data class OnDisconnect(val reason: Int, val peerId: String?) : WalletConnectStatus()