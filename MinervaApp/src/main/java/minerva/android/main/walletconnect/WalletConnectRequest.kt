package minerva.android.main.walletconnect

import minerva.android.walletmanager.model.DappSession

sealed class WalletConnectRequest()
data class OnEthSignRequest(val message: String, val session: DappSession) : WalletConnectRequest()
object DefaultRequest : WalletConnectRequest()