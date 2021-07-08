package minerva.android.walletmanager.model.token

import java.math.BigDecimal

interface TokenWithBalances {
    val token: Token
    val balance: BigDecimal
    val fiatBalance: BigDecimal
}