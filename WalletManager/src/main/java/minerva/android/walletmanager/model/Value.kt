package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import java.math.BigDecimal

data class Value(
    val index: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    val name: String = String.Empty,
    val network: String = String.Empty,
    val isDeleted: Boolean = false,
    var balance: BigDecimal = Int.InvalidId.toBigDecimal()
)