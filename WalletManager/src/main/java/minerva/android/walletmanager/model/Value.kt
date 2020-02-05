package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class Value(
    val index: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    var address: String = String.Empty,
    val name: String = String.Empty,
    val network: String = String.Empty,
    var isDeleted: Boolean = false,
    var balance: BigDecimal = BigDecimal.ZERO,
    var fiatBalance: BigDecimal = BigDecimal.ZERO
) {
    constructor(value: Value, isDeleted: Boolean) : this(
        value.index,
        value.publicKey,
        value.privateKey,
        value.address,
        value.name,
        value.network,
        isDeleted
    )
}