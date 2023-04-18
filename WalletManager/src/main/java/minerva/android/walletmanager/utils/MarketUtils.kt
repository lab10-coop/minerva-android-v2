package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_ONE
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_C
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.POLYGON
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZKS_ERA
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
        rate?.let {
            val multiplyValue = value.multiply(BigDecimal(it))

            if (multiplyValue > BalanceUtils.BIG_DECIMAL_ZERO && multiplyValue < BalanceUtils.ROUNDING_TO)
                multiplyValue //value got between range (from "$ 0.00...." to "< $ 0.01") and have to be rounded
            else
                multiplyValue.setScale(SCALE, RoundingMode.HALF_DOWN)
        }.orElse { Double.InvalidValue.toBigDecimal() }

    fun getRate(chainId: Int, markets: Markets, currentFiatCurrency: String): Double? =
        when (chainId) {
            ATS_SIGMA -> ATS_EURO
            POA_CORE -> markets.poaFiatPrice?.getRate(currentFiatCurrency)
            ETH_MAIN -> markets.ethFiatPrice?.getRate(currentFiatCurrency)
            ARB_ONE -> markets.ethFiatPrice?.getRate(currentFiatCurrency)
            OPT -> markets.ethFiatPrice?.getRate(currentFiatCurrency)
            GNO -> markets.daiFiatPrice?.getRate(currentFiatCurrency)
            POLYGON -> markets.polygonFiatPrice?.getRate(currentFiatCurrency)
            BSC -> markets.bscFiatPrice?.getRate(currentFiatCurrency)
            RSK_MAIN -> markets.rskFiatPrice?.getRate(currentFiatCurrency)
            CELO -> markets.celoFiatPrice?.getRate(currentFiatCurrency)
            AVA_C -> markets.avaxFiatPrice?.getRate(currentFiatCurrency)
            ZKS_ERA -> markets.ethFiatPrice?.getRate(currentFiatCurrency)
            else -> null
        }

    // coingecko coins
    fun getCoinGeckoMarketId(chainId: Int): String =
        when (chainId) {
            ETH_MAIN -> MarketIds.ETHEREUM
            POA_CORE -> MarketIds.POA_NETWORK
            GNO -> MarketIds.GNO
            POLYGON -> MarketIds.POLYGON
            BSC -> MarketIds.BSC_COIN
            RSK_MAIN -> MarketIds.RSK
            ARB_ONE -> MarketIds.ETHEREUM
            OPT -> MarketIds.ETHEREUM
            ZKS_ERA -> MarketIds.ETHEREUM
            CELO -> MarketIds.CELO
            AVA_C -> MarketIds.AVAX
            else -> String.Empty
        }
}