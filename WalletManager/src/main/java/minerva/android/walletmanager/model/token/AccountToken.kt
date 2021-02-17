package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class AccountToken(
    val token: ERC20Token,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
)