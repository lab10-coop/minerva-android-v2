package minerva.android.utils

import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

object BalanceUtils {

    fun getCryptoBalance(cryptoBalance: BigDecimal): String =
        if (cryptoBalance == Int.InvalidValue.toBigDecimal()) String.EmptyBalance
        else cryptoBalance.toPlainString()

    fun getFiatBalance(fiatBalance: BigDecimal): String =
        if (fiatBalance != Int.InvalidValue.toBigDecimal()) String.format(CURRENCY_FORMAT, fiatBalance)
        else NO_FIAT_VALUE

    private const val CURRENCY_FORMAT = "€ %.2f"
    private const val NO_FIAT_VALUE = "€ -.--"
}