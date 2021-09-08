package minerva.android.walletmanager.model.token

import java.math.BigDecimal

interface TokenWithBalances {
    val token: Token
    val currentBalance: BigDecimal
    val fiatBalance: BigDecimal
}