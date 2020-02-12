package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class Asset(
    val name: String = String.Empty,
    val nameShort: String = String.Empty,
    val address: String = String.Empty,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
)