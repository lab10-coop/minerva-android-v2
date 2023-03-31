package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletConnect.model.session.PEER_NAME
import minerva.android.walletConnect.model.session.PEER_URL

data class WalletConnectPeerMeta(
    val name: String = PEER_NAME,
    val url: String = PEER_URL,
    val description: String? = null,
    val icons: List<String> = listOf(),
    val peerId: String = String.Empty, // for walletconnect 1.0
    val address: String = String.Empty, // for walletconnect 1.0
    val chainId: Int = Int.InvalidId, // for walletconnect 1.0
    val proposerPublicKey: String = String.Empty, // for walletconnect 2.0
    val isMobileWalletConnect: Boolean = false,
    val handshakeId: Long = Long.InvalidValue
)
