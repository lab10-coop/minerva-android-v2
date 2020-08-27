package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

open class Service(
    override val type: String = String.Empty,
    override var name: String = String.Empty,
    override var lastUsed: Long = Long.InvalidValue,
    val loggedInIdentityPublicKey: String = String.Empty
) : MinervaPrimitive(name = name, type = type)