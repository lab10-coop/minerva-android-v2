package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue

data class WCPeerMeta(
    val name: String = PEER_NAME,
    val url: String = PEER_URL,
    val description: String? = null,
    val icons: List<String> = listOf(),
    val chainId: Int = Int.InvalidIndex,
    val peerId: String = String.Empty,
    val isMobileWalletConnect: Boolean = false,
    val handshakeId: Long = Long.InvalidValue
)

const val PEER_NAME = "Minerva Wallet"
const val PEER_URL = "https://docs.minerva.digital"