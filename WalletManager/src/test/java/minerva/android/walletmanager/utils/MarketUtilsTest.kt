package minerva.android.walletmanager.utils

import com.exchangemarketsprovider.model.Market
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.NetworkShortName
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals

class MarketUtilsTest {

    private val cryptoBalances: List<Pair<String, BigDecimal>> =
        listOf(Pair("address1", BigDecimal(12)), Pair("address2", BigDecimal(10)))
    private val poaNetwork = Network(testNet = false, short = NetworkShortName.POA_CORE)
    private val xdaiNetwork = Network(testNet = false, short = NetworkShortName.XDAI)

    private val poaTestNetwork = Network(testNet = true, short = NetworkShortName.POA_CORE)
    private val xdaiTestNetwork = Network(testNet = true, short = NetworkShortName.XDAI)
    private val testNetworksAccount =
        listOf(
            Account(1, address = "address1", network = poaTestNetwork),
            Account(2, address = "address2", network = xdaiTestNetwork)
        )
    private val accounts: List<Account> =
        listOf(Account(1, address = "address1", network = poaNetwork), Account(2, address = "address2", network = xdaiNetwork))
    private val markets = mutableListOf(Market(Markets.ETH_EUR, "2"), Market(Markets.POA_ETH, "3"), Market(Markets.ETH_DAI, "2"))

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets enabled`() {
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets)
        val expectedFiatBalance = BigDecimal.valueOf(72.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(expectedFiatBalance, result["address1"]?.fiatBalance)
        assertEquals(BigDecimal(10), result["address2"]?.cryptoBalance)
    }

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets disabled`() {
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, testNetworksAccount, markets)
        val expectedFiatBalance = BigDecimal.valueOf(72.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(expectedFiatBalance, result["address1"]?.fiatBalance)
        assertEquals(BigDecimal(10), result["address2"]?.cryptoBalance)
    }
}