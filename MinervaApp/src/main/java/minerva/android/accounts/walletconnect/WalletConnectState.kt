package minerva.android.accounts.walletconnect

import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction

sealed class WalletConnectState
object CloseScannerState : WalletConnectState()
object WrongQrCodeState : WalletConnectState()
object CorrectQrCodeState : WalletConnectState()
data class OnError(val error: Throwable) : WalletConnectState()
data class OnSessionRequestWithDefinedNetwork(val meta: WalletConnectPeerMeta, val network: String) :
    WalletConnectState()

data class OnSessionRequestWithUndefinedNetwork(val meta: WalletConnectPeerMeta, val network: String) :
    WalletConnectState()

object OnDisconnected : WalletConnectState()
data class ProgressBarState(val show: Boolean) : WalletConnectState()
data class UpdateDappsState(val dapps: List<DappSession>) : WalletConnectState()
object HideDappsState : WalletConnectState()
object OnSessionDeleted : WalletConnectState()
data class OnEthSignRequest(val message: String, val session: DappSession) : WalletConnectState()
data class OnEthSendTransactionRequest(val transaction: WalletConnectTransaction) : WalletConnectState()
object DefaultRequest : WalletConnectState()