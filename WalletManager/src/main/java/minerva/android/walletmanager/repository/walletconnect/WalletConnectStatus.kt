package minerva.android.walletmanager.repository.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.walletconnect.*

sealed class WalletConnectStatus
data class OnSessionRequest(val meta: WalletConnectPeerMeta, val chainId: Int?, val topic: Topic, val handshakeId: Long, val type: Int? = null) :
    WalletConnectStatus()
data class OnSessionRequestV2(val meta: WalletConnectPeerMeta, val numberOfNonEip155Chains: Int, val eip155ProposalNamespace: WalletConnectProposalNamespace): WalletConnectStatus()

data class OnDisconnect(val sessionName: String = String.Empty) : WalletConnectStatus()
data class OnEthSign(val message: String, val peerId: String) : WalletConnectStatus()
data class OnEthSignV2(val message: String, val session: DappSessionV2) : WalletConnectStatus()
sealed class OnEthSendTransaction(open val transaction: WalletConnectTransaction) : WalletConnectStatus()
data class OnEthSendTransactionV1(override val transaction: WalletConnectTransaction, val peerId: String) : OnEthSendTransaction(transaction)
data class OnEthSendTransactionV2(override val transaction: WalletConnectTransaction, val session: DappSessionV2) : OnEthSendTransaction(transaction)
data class OnFailure(val error: Throwable, val sessionName: String = String.Empty) : WalletConnectStatus()
