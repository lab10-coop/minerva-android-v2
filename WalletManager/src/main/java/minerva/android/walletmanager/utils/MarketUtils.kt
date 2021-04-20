package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Balance
import java.math.BigDecimal
import java.math.RoundingMode

object MarketUtils {

    private const val ATS_EURO = 0.073
    private const val SCALE = 2

    internal fun calculateFiatBalances(
        cryptoBalances: List<Pair<String, BigDecimal>>,
        accounts: List<Account>?,
        markets: Markets
    ): HashMap<String, Balance> {
        val balancesMap = hashMapOf<String, Balance>()
        accounts?.let {
            if (it.isNotEmpty()) {
                cryptoBalances.forEachIndexed { index, cryptoBalance ->
                    if (cryptoBalance.first == it[index]?.address) {
                        getBalance(balancesMap, cryptoBalance, getRate(accounts[index].network.chainId, markets))
                    }
                }
            }
        }
        return balancesMap
    }

    private fun getBalance(balances: HashMap<String, Balance>, cryptoBalance: Pair<String, BigDecimal>, rate: Double?) {
        balances[cryptoBalance.first] =
            Balance(cryptoBalance.second, fiatBalance = calculateFiatBalance(cryptoBalance.second, rate))
    }

    private fun calculateFiatBalance(value: BigDecimal, rate: Double?): BigDecimal =
        rate?.let { value.multiply(BigDecimal(it)).setScale(SCALE, RoundingMode.HALF_DOWN) }
            .orElse { Int.InvalidValue.toBigDecimal() }

    //TODO klop change it
    private fun getRate(chainId: Int, markets: Markets): Double? =
        when (chainId) {
            ATS_SIGMA -> ATS_EURO
            POA_CORE -> markets.poaFiatPrice?.getRate("EUR")
            ETH_MAIN -> markets.ethFiatPrice?.getRate("EUR")
            XDAI -> markets.daiFiatPrice?.getRate("EUR")
            else -> null
        }

    fun getMarketId(chainId: Int): String =
        when(chainId) {
            ETH_MAIN -> MarketIds.ETHEREUM
            POA_CORE -> MarketIds.POA_NETWORK
            XDAI -> MarketIds.XDAI
            else -> String.Empty
        }

    fun getMarketsIds(accounts: List<Account>?): String {
        var ids = String.Empty
        accounts?.distinctBy { it.network }?.forEach {
            when (it.network.chainId) {
                ETH_MAIN -> ids = "$ids${MarketIds.ETHEREUM},"
                POA_CORE -> ids = "$ids${MarketIds.POA_NETWORK},"
                XDAI -> ids = "$ids${MarketIds.XDAI},"
            }
        }
        return ids
    }
}