package minerva.android.walletmanager.model

import minerva.android.walletConnect.model.session.PEER_NAME
import minerva.android.walletConnect.model.session.PEER_URL

data class WalletConnectPeerMeta(
    val name: String = PEER_NAME,
    val url: String = PEER_URL,
    val description: String? = null,
    val icons: List<String> = listOf()
)
