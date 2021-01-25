package minerva.android.walletConnect.model.session

data class WCPeerMeta(
    val name: String = "Minerva Wallet",
    val url: String = "https://docs.minerva.digital",
    val description: String? = null,
    val icons: List<String> = listOf()
)