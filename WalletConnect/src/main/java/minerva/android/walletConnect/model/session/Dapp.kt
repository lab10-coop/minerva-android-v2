package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty

data class Dapp(
    val name: String = String.Empty,
    val icon: String = String.Empty,
    val peerId: String,
    val remotePeerId: String? = String.Empty
)