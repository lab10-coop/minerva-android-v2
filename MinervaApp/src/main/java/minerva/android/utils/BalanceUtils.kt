package minerva.android.utils

import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

object BalanceUtils {

    fun getCryptoBalance(cryptoBalance: BigDecimal): String =
        if (cryptoBalance == Int.InvalidValue.toBigDecimal()) WRONG_CRYPTO_STRING
        else cryptoBalance.toPlainString()

    fun getFiatBalance(fiatBalance: BigDecimal): String =
        if (fiatBalance != Int.InvalidValue.toBigDecimal()) String.format(CURRENCY_FORMAT, fiatBalance)
        else String.format(WRONG_CURRENCY_STRING, fiatBalance)

    private const val CURRENCY_FORMAT = "€ %.2f"
    private const val WRONG_CURRENCY_STRING = "€ -.--"
    private const val WRONG_CRYPTO_STRING = "-.--"
}