package minerva.android.walletmanager.utils

import com.exchangemarketsprovider.model.Market
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.defs.ExchangeRate
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.NetworkShortName
import java.math.BigDecimal
import java.math.RoundingMode

object MarketUtils {

    internal fun calculateFiatBalances(
        cryptoBalances: List<Pair<String, BigDecimal>>,
        values: List<Value>?,
        markets: MutableList<Market>
    ): HashMap<String, Balance> =
        hashMapOf<String, Balance>().apply {
            cryptoBalances.forEachIndexed { index, cryptoBalance ->
                if (cryptoBalance.first == values?.get(index)?.address) {
                    when (values[index].network) {
                        NetworkShortName.ATS -> getBalance(this, cryptoBalance, ExchangeRate.ATS_EURO)
                        NetworkShortName.ETH -> getBalance(this, cryptoBalance, getRate(markets)[Markets.ETH_EUR])
                        NetworkShortName.POA -> getBalance(this, cryptoBalance, getRate(markets)[Markets.POA_EUR])
                    }
                }
            }
        }

    private fun getBalance(balances: HashMap<String, Balance>, cryptoBalance: Pair<String, BigDecimal>, rate: Double?) {
        balances[cryptoBalance.first] = Balance(
            cryptoBalance.second,
            fiatBalance = (cryptoBalance.second.multiply(rate?.let { BigDecimal(it) })).setScale(SCALE, RoundingMode.HALF_DOWN)
        )
    }

    private fun getRate(markets: MutableList<Market>): HashMap<String, Double> {
        var poaEuroRate = Double.InvalidValue
        var ethEuroRate = Double.InvalidValue
        markets.forEach {
            when (it.symbol) {
                Markets.ETH_EUR -> ethEuroRate = it.price.toDouble()
                Markets.POA_ETH -> poaEuroRate = it.price.toDouble() * ethEuroRate
            }
        }
        return hashMapOf(Pair(Markets.ETH_EUR, ethEuroRate), Pair(Markets.POA_EUR, poaEuroRate))
    }

    fun getAddresses(values: List<Value>): List<Pair<String, String>> = values.map { it.network to it.address }

    private const val SCALE = 2
}