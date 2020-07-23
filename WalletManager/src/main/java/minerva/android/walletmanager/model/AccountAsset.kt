package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class AccountAsset(
    val asset: Asset,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
)