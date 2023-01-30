package minerva.android.walletmanager.repository.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectProposalNamespace
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

sealed class WalletConnectStatus
data class OnSessionRequest(val meta: WalletConnectPeerMeta, val chainId: Int?, val topic: Topic, val handshakeId: Long) :
    WalletConnectStatus()
data class OnSessionRequestV2(val meta: WalletConnectPeerMeta, val numberOfNonEip155Chains: Int, val eip155ProposalNamespace: WalletConnectProposalNamespace): WalletConnectStatus()

data class OnDisconnect(val sessionName: String = String.Empty) : WalletConnectStatus()
data class OnEthSign(val message: String, val peerId: String) : WalletConnectStatus()
data class OnEthSendTransaction(val transaction: WalletConnectTransaction, val peerId: String) : WalletConnectStatus()
data class OnFailure(val error: Throwable, val sessionName: String = String.Empty) : WalletConnectStatus()
