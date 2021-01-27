package minerva.android.walletConnect.model.session

data class WCPeerMeta(
    val name: String = PEER_NAME,
    val url: String = PEER_URL,
    val description: String? = null,
    val icons: List<String> = listOf()
)

const val PEER_NAME = "Minerva Wallet"
const val PEER_URL = "https://docs.minerva.digital"