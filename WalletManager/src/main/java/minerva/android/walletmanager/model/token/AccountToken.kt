package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal

data class AccountToken(
    val token: ERC20Token,
    var rawBalance: BigDecimal = Int.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue
) {
    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)?.let {
            token.address.equals(it.token.address, true)
        }.orElse { false }

    val balance: BigDecimal
        get() = if (rawBalance == BigDecimal.ZERO) BigDecimal.ZERO
        else BalanceUtils.fromWei(rawBalance, token.decimals.toInt())

    val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let {
                when (it) {
                    Double.InvalidValue -> WRONG_CURRENCY_VALUE
                    else -> BigDecimal(tokenPrice!!).multiply(balance)
                }
            }.orElse { WRONG_CURRENCY_VALUE }

    companion object {
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }
}