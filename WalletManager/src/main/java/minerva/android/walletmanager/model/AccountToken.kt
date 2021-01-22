package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class AccountToken(
    val token: Token,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
)