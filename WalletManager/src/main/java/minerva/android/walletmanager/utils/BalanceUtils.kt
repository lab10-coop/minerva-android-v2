package minerva.android.walletmanager.utils

import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow

object BalanceUtils {

    fun getCryptoBalance(cryptoBalance: BigDecimal): String =
        when {
            cryptoBalance == Double.InvalidValue.toBigDecimal() -> NO_VALUE
            cryptoBalance < MINIMAL_VALUE && cryptoBalance > BigDecimal.ZERO -> BELOW_MINIMAL_VALUE
            cryptoBalance == BigDecimal.ZERO -> String.EmptyBalance
            else -> cryptoBalance.setScale(CRYPTO_SCALE, RoundingMode.HALF_UP).let { scaled ->
                DecimalFormat(CRYPTO_FORMAT, DecimalFormatSymbols(Locale.ROOT)).format(scaled)
            }
        }

    fun getFiatBalance(fiatBalance: BigDecimal, fiatSymbol: String): String =
        when {
            fiatBalance < BigDecimal.ZERO -> String.format(Locale.ROOT, NO_FIAT_VALUE, fiatSymbol)
            fiatBalance != Double.InvalidValue.toBigDecimal() ->
                String.format(Locale.ROOT, CURRENCY_FORMAT, fiatSymbol, fiatBalance)
            fiatBalance == BigDecimal.ZERO -> ZERO
            else -> String.format(Locale.ROOT, NO_FIAT_VALUE, fiatSymbol)
        }

    fun convertFromWei(balance: BigDecimal, decimals: Int): BigDecimal =
        balance.divide(TEN.pow(decimals).toBigDecimal()).stripTrailingZeros()

    fun getSuperTokenFormatBalance(balance: BigDecimal): String =
        when {
            balance == Double.InvalidValue.toBigDecimal() -> NO_VALUE
            balance < MINIMAL_VALUE && balance > BigDecimal.ZERO -> BELOW_MINIMAL_VALUE
            balance == BigDecimal.ZERO -> String.EmptyBalance
            else -> DecimalFormat(SUPER_TOKEN_CRYPTO_FORMAT, DecimalFormatSymbols(Locale.ROOT)).format(balance.setScale(SUPER_TOKEN_CRYPTO_SCALE, RoundingMode.HALF_UP))
        }

    private const val CURRENCY_FORMAT = "%s %.2f"
    private const val NO_FIAT_VALUE = "%s -.--"
    private const val NO_VALUE = "-.--"
    private const val ZERO = "0"
    private const val TEN = 10.0
    private const val CRYPTO_SCALE = 10
    private const val CRYPTO_FORMAT = "#.##########"
    private const val BELOW_MINIMAL_VALUE = "<0.0000000001"
    private val MINIMAL_VALUE = 0.0000000001.toBigDecimal()
    private const val SUPER_TOKEN_CRYPTO_SCALE = 14
    private const val SUPER_TOKEN_CRYPTO_FORMAT = "0.00000000000000"

}