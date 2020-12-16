package minerva.android.walletmanager.utils

import com.exchangemarketsprovider.model.Market
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ExchangeRate
import minerva.android.walletmanager.model.defs.ExchangeRate.Companion.ATS_EURO
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.XDAI
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
            ATS_TAU, ATS_SIGMA -> ATS_EURO
            POA_SKL, POA_CORE -> getRatesMap(markets)[Markets.POA_EUR]
            ETH_RIN, ETH_KOV, ETH_GOR, ETH_ROP, ETH_MAIN  -> getRatesMap(markets)[Markets.ETH_EUR]
            XDAI -> getRatesMap(markets)[Markets.DAI_EUR]
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