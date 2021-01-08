package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.Price
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network
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
    private val markets = Markets(poaPrice = Price(value = 0.5), daiPrice = Price(2.0))

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets enabled`() {
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets)
        val expectedFiatBalance = BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(expectedFiatBalance, result["address1"]?.fiatBalance)
        assertEquals(BigDecimal(10), result["address2"]?.cryptoBalance)
    }

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets disabled`() {
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, testNetworksAccount, markets)
        val expectedFiatBalance = BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(expectedFiatBalance, result["address1"]?.fiatBalance)
        assertEquals(BigDecimal(10), result["address2"]?.cryptoBalance)
    }

    @Test
    fun `get market ids test`() {
        val accounts =
            listOf(
                Account(1, network = Network(short = "eth_mainnet")),
                Account(2, network = Network(short = "poa_core")),
                Account(3, network = Network(short = "xdai"))
            )
        val result = MarketUtils.getMarketsIds(accounts)
        assertEquals("ethereum,poa-network,dai,", result)
    }

    @Test
    fun `get market ids with no accounts test`() {
        val accounts = emptyList<Account>()
        val result = MarketUtils.getMarketsIds(accounts)
        assertEquals("", result)
    }
}