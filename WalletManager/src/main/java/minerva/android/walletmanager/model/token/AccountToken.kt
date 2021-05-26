package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    var token: ERC20Token,
    var rawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue
) {
    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)
            ?.let { accountToken -> token.address.equals(accountToken.token.address, true) }
            .orElse { false }

    val balance: BigDecimal
        get() = if (rawBalance ==  Double.InvalidValue.toBigDecimal()) BigDecimal.ZERO
        else BalanceUtils.convertFromWei(rawBalance, token.decimals.toInt())

    val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let { price ->
                when (price) {
                    Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
                    else -> BigDecimal(price).multiply(balance).setScale(FIAT_SCALE, RoundingMode.HALF_UP)
                }
            }.orElse { Double.InvalidValue.toBigDecimal() }

    companion object {
        private const val FIAT_SCALE = 13
    }
}