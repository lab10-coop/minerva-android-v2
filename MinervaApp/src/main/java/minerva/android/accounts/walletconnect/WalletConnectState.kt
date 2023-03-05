package minerva.android.accounts.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.*

// todo: why is this duplicate with ServicesScannerViewState?

sealed class WalletConnectState
object CloseScannerState : WalletConnectState()
object CloseDialogState : WalletConnectState()
object WrongWalletConnectCodeState : WalletConnectState()
object CorrectWalletConnectCodeState : WalletConnectState()
data class OnGeneralError(val error: Throwable, val sessionName: String = String.Empty) : WalletConnectState()
data class OnWalletConnectTransactionError(val error: Throwable) : WalletConnectState()
data class OnWalletConnectConnectionError(val error: Throwable, val sessionName: String = String.Empty) :
    WalletConnectState()

data class OnSessionRequest(
    val meta: WalletConnectPeerMeta,
    val network: BaseNetworkData,
    val dialogType: WalletConnectAlertType
) : WalletConnectState()
data class OnSessionRequestV2(
    val meta: WalletConnectPeerMeta,
    val numberOfNonEip155Chains: Int,
    val eip155ProposalNamespace: WalletConnectProposalNamespace
) : WalletConnectState()

data class OnDisconnected(val sessionName: String = String.Empty) : WalletConnectState()
data class ProgressBarState(val show: Boolean) : WalletConnectState()
data class OnEthSignRequest(val message: String, val session: DappSessionV1) : WalletConnectState()
data class OnEthSignRequestV2(val message: String, val session: DappSessionV2) : WalletConnectState()
data class OnEthSendTransactionRequest(
    val transaction: WalletConnectTransaction,
    val session: DappSession,
    val account: Account?
) : WalletConnectState()

object DefaultRequest : WalletConnectState()
data class WrongTransactionValueState(val transaction: WalletConnectTransaction) : WalletConnectState()
data class UpdateOnSessionRequest(val network: BaseNetworkData, val dialogType: WalletConnectAlertType) : WalletConnectState()