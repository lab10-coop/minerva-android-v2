package minerva.android.accounts.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

sealed class WalletConnectState
object CloseScannerState : WalletConnectState()
object WrongQrCodeState : WalletConnectState()
object CorrectQrCodeState : WalletConnectState()
data class OnGeneralError(val error: Throwable, val sessionName: String = String.Empty) : WalletConnectState()
data class OnWalletConnectConnectionError(val error: Throwable, val sessionName: String = String.Empty) :
    WalletConnectState()

data class OnSessionRequest(
    val meta: WalletConnectPeerMeta,
    val network: String,
    val dialogType: WalletConnectAlertType
) : WalletConnectState()

data class OnDisconnected(val sessionName: String = String.Empty) : WalletConnectState()
data class ProgressBarState(val show: Boolean) : WalletConnectState()
data class UpdateDappsState(val dapps: List<DappSession>) : WalletConnectState()
object HideDappsState : WalletConnectState()
object OnSessionDeleted : WalletConnectState()
data class OnEthSignRequest(val message: String, val session: DappSession) : WalletConnectState()
data class OnEthSendTransactionRequest(
    val transaction: WalletConnectTransaction,
    val session: DappSession,
    val account: Account?
) : WalletConnectState()

object DefaultRequest : WalletConnectState()
data class WrongTransactionValueState(val transaction: WalletConnectTransaction) : WalletConnectState()