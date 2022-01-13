package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    override var token: ERCToken,
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
        get() = if (token.type.isNft() || token.decimals.isBlank()) currentRawBalance else getBalanceForERC20Token(
            currentRawBalance
        )

    val nextBalance: BigDecimal
        get() = if (token.type.isNft() || token.decimals.isBlank()) nextRawBalance else getBalanceForERC20Token(nextRawBalance)

    override val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let { price ->
                when (price) {
                    Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
                    else -> BigDecimal(price).multiply(currentBalance).setScale(FIAT_SCALE, RoundingMode.HALF_UP)
                }
            }.orElse { Double.InvalidValue.toBigDecimal() }


    private fun getBalanceForERC20Token(rawBalance: BigDecimal) =
        if (rawBalance == Double.InvalidValue.toBigDecimal()) rawBalance
        else BalanceUtils.convertFromWei(rawBalance, token.decimals.toInt())


    companion object {
        private const val FIAT_SCALE = 13
    }
}