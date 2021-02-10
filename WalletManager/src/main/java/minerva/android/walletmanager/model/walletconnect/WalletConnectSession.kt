package minerva.android.walletmanager.model.walletconnect

data class WalletConnectSession(
    val topic: String,
    val version: String,
    val bridge: String,
    val key: String
)
