package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty

data class Topic(
    val peerId: String = String.Empty,
    val remotePeerId: String? = String.Empty
)
