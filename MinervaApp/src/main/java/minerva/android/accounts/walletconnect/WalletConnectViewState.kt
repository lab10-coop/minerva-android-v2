package minerva.android.accounts.walletconnect

import minerva.android.walletConnect.model.session.WCPeerMeta

sealed class WalletConnectViewState
object CloseScannerState : WalletConnectViewState()
object WrongQrCodeState : WalletConnectViewState()
object CorrectQrCodeState : WalletConnectViewState()
data class OnError(val error: Throwable) : WalletConnectViewState()
data class OnSessionRequestWithDefinedNetwork(val meta: WCPeerMeta, val network: String) :
    WalletConnectViewState()
data class OnSessionRequestWithUndefinedNetwork(val meta: WCPeerMeta, val network: String) :
    WalletConnectViewState()
data class OnDisconnected(val reason: Int) : WalletConnectViewState()
data class ProgressBarState(val show: Boolean) : WalletConnectViewState()