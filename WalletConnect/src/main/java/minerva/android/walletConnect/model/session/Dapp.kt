package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty

data class Dapp( // change name to DappSession
//    val address: String? = String.Empty, says tha it belongs to given account
//    val topic: String,
//    val version: String,
//    val bridge: String,
//    val key: String,


    val name: String = String.Empty,
    val icon: String = String.Empty,
    val peerId: String,
    val remotePeerId: String? = String.Empty

)