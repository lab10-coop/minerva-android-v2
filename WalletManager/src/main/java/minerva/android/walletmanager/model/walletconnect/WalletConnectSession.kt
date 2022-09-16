package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty

data class WalletConnectSession(
    val topic: String = String.Empty,
    val version: String = String.Empty,
    val bridge: String = String.Empty,
    val key: String = String.Empty
)
