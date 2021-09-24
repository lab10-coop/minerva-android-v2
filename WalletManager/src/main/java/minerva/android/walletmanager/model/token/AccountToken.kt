package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    override var token: ERC20Token,
    var currentRawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue,
    var nextRawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var isInitStream: Boolean = false
) : TokenWithBalances {

    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)
            ?.let { accountToken -> token.address.equals(accountToken.token.address, true) }
            .orElse { false }

    override val currentBalance: BigDecimal
        get() = if (currentRawBalance == Double.InvalidValue.toBigDecimal()) currentRawBalance
        else BalanceUtils.convertFromWei(currentRawBalance, token.decimals.toInt())

    val nextBalance: BigDecimal
        get() = if (nextRawBalance == Double.InvalidValue.toBigDecimal()) nextRawBalance
        else BalanceUtils.convertFromWei(nextRawBalance, token.decimals.toInt())

    override val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let { price ->
                when (price) {
                    Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
                    else -> BigDecimal(price).multiply(currentBalance).setScale(FIAT_SCALE, RoundingMode.HALF_UP)
                }
            }.orElse { Double.InvalidValue.toBigDecimal() }

    companion object {
        private const val FIAT_SCALE = 13
    }
}