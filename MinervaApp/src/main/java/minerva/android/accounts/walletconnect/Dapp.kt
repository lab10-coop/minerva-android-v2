package minerva.android.accounts.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId

data class Dapp(
    val name: String = String.Empty,
    val icon: String = String.Empty,
    val peerId: String
    //todo add remotePeerId for reconnection ??
)