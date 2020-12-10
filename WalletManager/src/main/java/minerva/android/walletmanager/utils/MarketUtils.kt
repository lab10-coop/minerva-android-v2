package minerva.android.walletmanager.utils

import com.exchangemarketsprovider.model.Market
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ExchangeRate
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.NetworkShortName
import java.math.BigDecimal
import java.math.RoundingMode

object MarketUtils {

    internal fun calculateFiatBalances(
        cryptoBalances: List<Pair<String, BigDecimal>>,
        accounts: List<Account>?,
        markets: MutableList<Market>
    ): HashMap<String, Balance> {
        val balancesMap = hashMapOf<String, Balance>()
        cryptoBalances.forEachIndexed { index, cryptoBalance ->
            if (cryptoBalance.first == accounts?.get(index)?.address) {
                getBalance(balancesMap, cryptoBalance, getRate(accounts[index].network.short, markets))
            }
        }
        return balancesMap
    }

    private fun getBalance(balances: HashMap<String, Balance>, cryptoBalance: Pair<String, BigDecimal>, rate: Double?) {
        balances[cryptoBalance.first] = Balance(cryptoBalance.second, fiatBalance = countFiatBalance(cryptoBalance.second, rate))
    }

    private fun countFiatBalance(value: BigDecimal, rate: Double?): BigDecimal =
        rate?.let { value.multiply(BigDecimal(it)).setScale(SCALE, RoundingMode.HALF_DOWN) }
            .orElse { Int.InvalidValue.toBigDecimal() }

    private fun getRate(network: String, markets: MutableList<Market>): Double? =
        //TODO make ratings in dynamical way
        when (network) {
            NetworkShortName.ATS_TAU, NetworkShortName.ATS_SIGMA -> ExchangeRate.ATS_EURO
            NetworkShortName.POA_SKL, NetworkShortName.POA_CORE -> getRatesMap(markets)[Markets.POA_EUR]
            NetworkShortName.ETH_RIN, NetworkShortName.ETH_KOV,
            NetworkShortName.ETH_GOR, NetworkShortName.ETH_ROP -> getRatesMap(markets)[Markets.ETH_EUR]
            NetworkShortName.XDAI -> getRatesMap(markets)[Markets.DAI_EUR]
            else -> null
        }

    private fun getRatesMap(markets: MutableList<Market>): HashMap<String, Double> {
        var poaEuroRate = Double.InvalidValue
        var ethEuroRate = Double.InvalidValue
        var daiEuroRate = Double.InvalidValue
        markets.forEach {
            when (it.symbol) {
                Markets.ETH_EUR -> ethEuroRate = it.price.toDouble()
                Markets.POA_ETH -> poaEuroRate = it.price.toDouble() * ethEuroRate
                Markets.ETH_DAI -> daiEuroRate = ethEuroRate / it.price.toDouble()
            }
        }
        return hashMapOf(
            Pair(Markets.ETH_EUR, ethEuroRate),
            Pair(Markets.POA_EUR, poaEuroRate),
            Pair(Markets.DAI_EUR, daiEuroRate)
        )
    }

    private const val SCALE = 2
}