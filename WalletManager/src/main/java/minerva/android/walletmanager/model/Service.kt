package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Service(
    override val type: String = String.Empty,
    override var name: String = String.Empty,
    var lastUsed: String = String.Empty,
    val loggedInIdentityPublicKey: String = String.Empty
) : MinervaPrimitive(name = name, type = type)