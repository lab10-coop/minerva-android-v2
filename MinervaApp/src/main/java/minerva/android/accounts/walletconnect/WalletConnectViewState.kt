package minerva.android.accounts.walletconnect

import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletmanager.model.DappSession

sealed class WalletConnectViewState
object CloseScannerState : WalletConnectViewState()
object WrongQrCodeState : WalletConnectViewState()
object CorrectQrCodeState : WalletConnectViewState()
data class OnError(val error: Throwable, val peerId: String? = null) : WalletConnectViewState()
data class OnSessionRequestWithDefinedNetwork(val meta: WCPeerMeta, val network: String) :
    WalletConnectViewState()

data class OnSessionRequestWithUndefinedNetwork(val meta: WCPeerMeta, val network: String) :
    WalletConnectViewState()

data class OnDisconnected(val peerId: String?) : WalletConnectViewState()
data class ProgressBarState(val show: Boolean) : WalletConnectViewState()
data class UpdateDappsState(val dapps: List<DappSession>) : WalletConnectViewState()
object HideDappsState : WalletConnectViewState()