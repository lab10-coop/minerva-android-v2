package minerva.android.walletConnect.model.session

import minerva.android.kotlinUtils.Empty

data class ConnectedDapps(
    val address: String = String.Empty,
    val dapps: MutableList<Dapp> = mutableListOf()
)
