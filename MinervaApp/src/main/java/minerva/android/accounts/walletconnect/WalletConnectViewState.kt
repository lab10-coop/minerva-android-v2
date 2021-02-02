package minerva.android.accounts.walletconnect

import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.WalletConnectPeerMeta

sealed class WalletConnectViewState
object CloseScannerState : WalletConnectViewState()
object WrongQrCodeState : WalletConnectViewState()
object CorrectQrCodeState : WalletConnectViewState()
data class OnError(val error: Throwable) : WalletConnectViewState()
data class OnSessionRequestWithDefinedNetwork(val meta: WalletConnectPeerMeta, val network: String) :
    WalletConnectViewState()

data class OnSessionRequestWithUndefinedNetwork(val meta: WalletConnectPeerMeta, val network: String) :
    WalletConnectViewState()

object OnDisconnected : WalletConnectViewState()
data class ProgressBarState(val show: Boolean) : WalletConnectViewState()
data class UpdateDappsState(val dapps: List<DappSession>) : WalletConnectViewState()
object HideDappsState : WalletConnectViewState()
object OnSessionDeleted : WalletConnectViewState()