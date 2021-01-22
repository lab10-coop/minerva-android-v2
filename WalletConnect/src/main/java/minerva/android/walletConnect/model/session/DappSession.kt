package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty

data class DappSession(
    val address: String = String.Empty,
    val topic: String = String.Empty,
    val version: String = String.Empty,
    val bridge: String = String.Empty,
    val key: String = String.Empty,
    val name: String = String.Empty,
    val icon: String = String.Empty,
    val peerId: String = String.Empty,
    val remotePeerId: String? = String.Empty
)
