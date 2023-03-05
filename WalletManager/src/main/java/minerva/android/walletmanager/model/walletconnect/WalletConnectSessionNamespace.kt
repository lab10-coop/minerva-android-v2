package minerva.android.walletmanager.model.walletconnect

data class WalletConnectSessionNamespace(
    val chains: List<String>?,
    val accounts: List<String>,
    val methods: List<String>,
    val events: List<String>
)
