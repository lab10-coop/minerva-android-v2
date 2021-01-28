package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.token.ERC20Token
import java.math.BigDecimal

data class AccountToken(
    val token: ERC20Token,
    var balance: BigDecimal = Int.InvalidValue.toBigDecimal()
)