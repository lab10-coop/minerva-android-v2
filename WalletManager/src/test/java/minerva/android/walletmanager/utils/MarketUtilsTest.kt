package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.FiatPrice
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals

class MarketUtilsTest {

    private val cryptoBalances: List<Pair<String, BigDecimal>> =
        listOf(Pair("address1", BigDecimal(12)), Pair("address2", BigDecimal(10)))
    private val poaNetwork = Network(testNet = false, chainId = POA_CORE, httpRpc = "some")
    private val xdaiNetwork = Network(testNet = false, chainId = XDAI, httpRpc = "some")

    private val poaTestNetwork = Network(testNet = true, chainId = POA_CORE)
    private val xdaiTestNetwork = Network(testNet = true, chainId = XDAI)

    private val testNetworksAccount =
        listOf(
            Account(1, address = "address1", chainId = poaTestNetwork.chainId),
            Account(2, address = "address2", chainId = xdaiTestNetwork.chainId)
        )
    private val accounts: List<Account> =
        listOf(
            Account(1, address = "address1", chainId = poaNetwork.chainId),
            Account(2, address = "address2", chainId = xdaiNetwork.chainId)
        )
    private val markets = Markets(poaFiatPrice = FiatPrice(eur = 0.5), daiFiatPrice = FiatPrice(2.0))

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
                Network(chainId = 1, httpRpc = "some"),
                Network(chainId = 99, httpRpc = "some"),
                Network(chainId = 100, httpRpc = "some")
            )
        )
        val accounts =
            listOf(
                Account(1, chainId = 1),
                Account(2, chainId = 99),
                Account(3, chainId = 100)
            )
        val result = MarketUtils.getMarketsIds(accounts)
        assertEquals("ethereum,poa-network,xdai,", result)
    }

    @Test
    fun `get market ids with no accounts test`() {
        val accounts = emptyList<Account>()
        val result = MarketUtils.getMarketsIds(accounts)
        assertEquals("", result)
    }
}