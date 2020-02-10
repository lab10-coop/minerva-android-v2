package minerva.android.walletmanager.utils

import com.exchangemarketprovider.model.Market
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.*
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
                        NetworkNameShort.ATS -> getBalance(this, cryptoBalance, ExchangeRate.ATS_EURO)
                        NetworkNameShort.ETH -> getBalance(this, cryptoBalance, getRate(markets)[Markets.ETH_EUR])
                        NetworkNameShort.POA -> getBalance(this, cryptoBalance, getRate(markets)[Markets.POA_EUR])
                        NetworkNameShort.XDAI -> getBalance(this, cryptoBalance, ExchangeRate.XDAI_EURO)
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

    fun getAddresses(values: List<Value>?): MutableList<String> =
        mutableListOf<String>().apply {
            values?.forEach { add(it.address) }
        }

    private const val SCALE = 2
}