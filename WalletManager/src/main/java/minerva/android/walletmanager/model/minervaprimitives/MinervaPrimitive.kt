package minerva.android.walletmanager.model.minervaprimitives

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue

open class MinervaPrimitive(
    open val address: String = String.Empty,
    open var name: String = String.Empty,
    open var isDeleted: Boolean = false,
    open val bindedOwner: String = String.Empty,
    open var lastUsed: Long = Long.InvalidValue,
    open val iconUrl: String? = String.Empty,
    open var isHide: Boolean = false,
    open val peerId: String = String.Empty,
    open val accountName: String = String.Empty,
    open val chainId: Int = Int.InvalidId
) {
    val isSafeAccount: Boolean
        get() = bindedOwner != String.Empty
}