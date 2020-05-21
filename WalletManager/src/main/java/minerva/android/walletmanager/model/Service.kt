package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Service(
    val type: String = String.Empty,
    val name: String = String.Empty,
    var lastUsed: String = String.Empty,
    val loggedInIdentityPublicKey: String = String.Empty
)