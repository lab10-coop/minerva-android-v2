package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.minervaprimitives.account.CoinCryptoBalance
import minerva.android.walletmanager.model.transactions.Balance
import java.math.BigDecimal
import java.math.RoundingMode

object MarketUtils {

    private const val ATS_EURO = 0.073
    private const val SCALE = 2

    internal fun calculateFiatBalance(
        cryptoBalance: CoinCryptoBalance,
        markets: Markets,
        currentFiat: String
    ): CoinBalance = with(cryptoBalance) {
        return if (balance == BigDecimal.ZERO) {
            CoinBalance(chainId, address, Balance(cryptoBalance = balance, fiatBalance = BigDecimal.ZERO), null)
        } else {
            val rate = getRate(chainId, markets, currentFiat)
            CoinBalance(
                chainId,
                address,
                Balance(cryptoBalance = balance, fiatBalance = calculateFiatBalance(balance, rate)),
                rate = rate
            )
        }
    }

    fun calculateFiatBalance(value: BigDecimal, rate: Double?): BigDecimal =
        rate?.let { value.multiply(BigDecimal(it)).setScale(SCALE, RoundingMode.HALF_DOWN) }
            .orElse { Double.InvalidValue.toBigDecimal() }

    fun getRate(chainId: Int, markets: Markets, currentFiatCurrency: String): Double? =
        when (chainId) {
            ATS_SIGMA -> ATS_EURO
            POA_CORE -> markets.poaFiatPrice?.getRate(currentFiatCurrency)
            ETH_MAIN -> markets.ethFiatPrice?.getRate(currentFiatCurrency)
            XDAI -> markets.daiFiatPrice?.getRate(currentFiatCurrency)
            MATIC -> markets.maticFiatPrice?.getRate(currentFiatCurrency)
            BSC -> markets.bscFiatPrice?.getRate(currentFiatCurrency)
            else -> null
        }

    fun getTokenGeckoMarketId(chainId: Int): String =
        when (chainId) {
            ETH_MAIN -> MarketIds.ETHEREUM
            POA_CORE -> MarketIds.POA_NETWORK
            XDAI -> MarketIds.XDAI
            MATIC -> MarketIds.POLYGON
            BSC -> MarketIds.BSC
            else -> String.Empty
        }

    fun getCoinGeckoMarketId(chainId: Int): String =
        when (chainId) {
            ETH_MAIN -> MarketIds.ETHEREUM
            POA_CORE -> MarketIds.POA_NETWORK
            XDAI -> MarketIds.XDAI
            MATIC -> MarketIds.MATIC
            BSC -> MarketIds.BSC
            else -> String.Empty
        }
}