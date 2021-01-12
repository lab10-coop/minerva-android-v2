package minerva.android.walletConnect.model.session

data class WCPeerMeta(
    val name: String,
    val url: String,
    val description: String? = null,
    val icons: List<String> = listOf("")
)