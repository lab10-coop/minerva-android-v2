package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class Credential(
    override var name: String = String.Empty,
    override val type: String = String.Empty,
    val issuer: String = String.Empty,
    val memberName: String = String.Empty,
    val memberId: String = String.Empty,
    val coverage: String = String.Empty,
    val expirationDate: Long = Long.InvalidValue,
    val creationDate: String = String.Empty,
    val loggedInIdentityDid: String = String.Empty,
    var lastUsed: String = String.Empty
) : MinervaPrimitive(name = name, type = type)