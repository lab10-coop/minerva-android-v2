package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.Price
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.NetworkShortName
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals

class MarketUtilsTest {

    private val cryptoBalances: List<Pair<String, BigDecimal>> =
        listOf(Pair("address1", BigDecimal(12)), Pair("address2", BigDecimal(10)))
    private val poaNetwork = Network(testNet = false, short = NetworkShortName.POA_CORE, httpRpc = "some")
    private val xdaiNetwork = Network(testNet = false, short = NetworkShortName.XDAI, httpRpc = "some")

    private val poaTestNetwork = Network(testNet = true, short = NetworkShortName.POA_CORE)
    private val xdaiTestNetwork = Network(testNet = true, short = NetworkShortName.XDAI)

    private val testNetworksAccount =
        listOf(
            Account(1, address = "address1", networkShort = poaTestNetwork.short),
            Account(2, address = "address2", networkShort = xdaiTestNetwork.short)
        )
    private val accounts: List<Account> =
        listOf(
            Account(1, address = "address1", networkShort = poaNetwork.short),
            Account(2, address = "address2", networkShort = xdaiNetwork.short)
        )
    private val markets = Markets(poaPrice = Price(value = 0.5), daiPrice = Price(2.0))

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets enabled`() {
        NetworkManager.initialize(listOf(poaNetwork, xdaiNetwork))
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
        NetworkManager.initialize(
            listOf(
                Network(short = "eth_mainnet", httpRpc = "some"),
                Network(short = "poa_core", httpRpc = "some"),
                Network(short = "xdai", httpRpc = "some")
            )
        )
        val accounts =
            listOf(
                Account(1, networkShort = "eth_mainnet"),
                Account(2, networkShort = "poa_core"),
                Account(3, networkShort = "xdai")
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