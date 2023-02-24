package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletConnect.model.session.PEER_NAME
import minerva.android.walletConnect.model.session.PEER_URL

data class WalletConnectPeerMeta(
    val name: String = PEER_NAME,
    val url: String = PEER_URL,
    val description: String? = null,
    val icons: List<String> = listOf(),
    val peerId: String = String.Empty,
    val address: String = String.Empty,
    val chainId: Int = Int.InvalidId,
    val isMobileWalletConnect: Boolean = false
)
