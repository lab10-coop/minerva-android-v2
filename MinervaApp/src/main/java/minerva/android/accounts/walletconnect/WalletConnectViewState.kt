package minerva.android.accounts.walletconnect

import minerva.android.walletConnect.model.session.WCPeerMeta

sealed class WalletConnectViewState
object CloseScannerState : WalletConnectViewState()
object WrongQrCodeState : WalletConnectViewState()
object CorrectQrCodeState : WalletConnectViewState()
data class OnError(val error: Throwable) : WalletConnectViewState()
data class OnWCSessionRequest(val meta: WCPeerMeta, val chainId: String?) : WalletConnectViewState()
data class OnWCDisconnected(val reason: Int) : WalletConnectViewState()