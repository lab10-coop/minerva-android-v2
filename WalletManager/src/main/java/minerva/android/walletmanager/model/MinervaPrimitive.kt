package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

open class MinervaPrimitive(
    open val address: String = String.Empty,
    open var name: String = String.Empty,
    open var isDeleted: Boolean = false,
    open val bindedOwner: String = String.Empty,
    open val network: String = String.Empty,
    open val type: String = String.Empty,
    open var lastUsed: Long = Long.InvalidValue
) {
    val isSafeAccount: Boolean
        get() = bindedOwner != String.Empty
}