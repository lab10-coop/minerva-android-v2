package minerva.android.accounts.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

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

data class OnDisconnected(val sessionName: String = String.Empty) : WalletConnectState()
data class ProgressBarState(val show: Boolean) : WalletConnectState()
object HideDappsState : WalletConnectState()
data class OnEthSignRequest(val message: String, val session: DappSessionV1) : WalletConnectState()
data class OnEthSendTransactionRequest(
    val transaction: WalletConnectTransaction,
    val session: DappSessionV1,
    val account: Account?
) : WalletConnectState()

object DefaultRequest : WalletConnectState()
data class WrongTransactionValueState(val transaction: WalletConnectTransaction) : WalletConnectState()
data class UpdateOnSessionRequest(val network: BaseNetworkData, val dialogType: WalletConnectAlertType) : WalletConnectState()

/**
 * To Service Fragment Request - state which transfers to ServicesFragment
 */
object ToServiceFragmentRequest : WalletConnectState()