package minerva.android.walletmanager.utils

import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow

object BalanceUtils {

    fun getCryptoBalance(cryptoBalance: BigDecimal): String =
        if (cryptoBalance == Int.InvalidValue.toBigDecimal()) String.EmptyBalance
        else cryptoBalance.toPlainString()

    fun getFiatBalance(fiatBalance: BigDecimal): String =
        if (fiatBalance != Int.InvalidValue.toBigDecimal()) String.format(Locale.ROOT, CURRENCY_FORMAT, fiatBalance)
        else NO_FIAT_VALUE

    fun fromWei(balance: BigDecimal, decimals: Int) =
        balance / (10.0.pow(decimals)).toBigDecimal()

    private const val CURRENCY_FORMAT = "€ %.2f"
    private const val NO_FIAT_VALUE = "€ -.--"
}