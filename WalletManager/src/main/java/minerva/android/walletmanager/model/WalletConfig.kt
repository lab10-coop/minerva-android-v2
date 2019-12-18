package minerva.android.walletmanager.model

data class WalletConfig(
    val identities: List<Identity> = listOf(),
    val values: List<Value> = listOf()
)