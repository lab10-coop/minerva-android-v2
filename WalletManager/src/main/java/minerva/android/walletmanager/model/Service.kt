package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

open class Service(
    val issuer: String = String.Empty,
    override var name: String = String.Empty,
    override var lastUsed: Long = Long.InvalidValue,
    val loggedInIdentityPublicKey: String = String.Empty,
    override val iconUrl: String = String.Empty
) : MinervaPrimitive(name = name, lastUsed = lastUsed)