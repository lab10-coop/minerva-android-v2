package minerva.android.walletmanager.model

import minerva.android.apiProvider.model.FiatPrice
import java.util.*
import kotlin.reflect.full.memberProperties

object Fiat {
    val all: List<String> by lazy {
        mutableListOf<String>().apply {
            FiatPrice::class.memberProperties.forEach { add(it.name.toUpperCase(Locale.ROOT)) }
            remove(USD)
            add(FIRST_INDEX, USD)
            remove(GBP)
            add(FIRST_INDEX, GBP)
            remove(EUR)
            add(FIRST_INDEX, EUR)
        }
    }


    fun getFiatSymbol(fiat: String): String =
        when (fiat) {
            EUR -> EUR_SYMBOL
            GBP -> GBP_SYMBOL
            USD -> USD_SYMBOL
            else -> fiat
        }

    const val EUR = "EUR"
    const val GBP = "GBP"
    const val USD = "USD"
    private const val EUR_SYMBOL = "€"
    private const val GBP_SYMBOL = "£"
    private const val USD_SYMBOL = "$"
    private const val FIRST_INDEX = 0
}