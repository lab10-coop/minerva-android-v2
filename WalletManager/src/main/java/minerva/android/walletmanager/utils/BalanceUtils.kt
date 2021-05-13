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
        if (cryptoBalance == Double.InvalidValue.toBigDecimal()) String.EmptyBalance
        else {
            val scaled = cryptoBalance.setScale(CRYPTO_SCALE, RoundingMode.CEILING)
            DecimalFormat(CRYPTO_FORMAT, DecimalFormatSymbols(Locale.ROOT)).format(scaled)
        }

    fun getFiatBalance(fiatBalance: BigDecimal, fiatSymbol: String): String =
        if (fiatBalance != Double.InvalidValue.toBigDecimal()) String.format(Locale.ROOT, CURRENCY_FORMAT, fiatSymbol, fiatBalance)
        else String.format(Locale.ROOT, NO_FIAT_VALUE, fiatSymbol)

    fun convertFromWei(balance: BigDecimal, decimals: Int): BigDecimal =
        balance.divide(TEN.pow(decimals).toBigDecimal()).stripTrailingZeros()

    private const val CURRENCY_FORMAT = "%s %.2f"
    private const val NO_FIAT_VALUE = "%s -.--"
    private const val TEN = 10.0
    private const val CRYPTO_SCALE = 6
    private const val CRYPTO_FORMAT = "#.######"
}