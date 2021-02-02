package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Topic(
    val peerId: String = String.Empty,
    val remotePeerId: String? = String.Empty
)
