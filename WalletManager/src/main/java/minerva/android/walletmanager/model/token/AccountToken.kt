package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    val token: ERC20Token,
    var rawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue
) {
    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)?.let {
            token.address.equals(it.token.address, true)
        }.orElse { false }

    val balance: BigDecimal
        get() = if (rawBalance == BigDecimal.ZERO) BigDecimal.ZERO
        else BalanceUtils.convertFromWei(rawBalance, token.decimals.toInt())

    val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let {
                when (it) {
                    Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
                    else -> BigDecimal(it).multiply(balance).setScale(13, RoundingMode.HALF_UP)
                }
            }.orElse { Double.InvalidValue.toBigDecimal() }
}