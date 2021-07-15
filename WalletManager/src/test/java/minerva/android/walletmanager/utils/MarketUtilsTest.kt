package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.FiatPrice
import minerva.android.apiProvider.model.Markets
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals

class MarketUtilsTest {

    private val poaNetwork = Network(testNet = false, chainId = POA_CORE, httpRpc = "some")
    private val xdaiNetwork = Network(testNet = false, chainId = XDAI, httpRpc = "some")

    private val poaTestNetwork = Network(testNet = true, chainId = POA_CORE)
    private val xdaiTestNetwork = Network(testNet = true, chainId = XDAI)

    private val cryptoBalances: List<Triple<Int, String, BigDecimal>> =
        listOf(Triple( poaNetwork.chainId, "address1", BigDecimal(12)), Triple(xdaiNetwork.chainId, "address2", BigDecimal(10)))

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
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets, "EUR")
        val expectedFiatBalance = BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(
            expectedFiatBalance,
            result.find { coinBalance -> coinBalance.chainId == poaNetwork.chainId && coinBalance.address == "address1" }?.balance?.fiatBalance
        )
        assertEquals(
            BigDecimal(10),
            result.find { coinBalance -> coinBalance.chainId == xdaiNetwork.chainId && coinBalance.address == "address2" }?.balance?.cryptoBalance
        )
    }

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets disabled`() {
        val result = MarketUtils.calculateFiatBalances(cryptoBalances, testNetworksAccount, markets, "EUR")
        val expectedFiatBalance = BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(
            expectedFiatBalance,
            result.find { coinBalance -> coinBalance.chainId == poaNetwork.chainId && coinBalance.address == "address1" }?.balance?.fiatBalance
        )
        assertEquals(
            BigDecimal(10),
            result.find { coinBalance -> coinBalance.chainId == xdaiNetwork.chainId && coinBalance.address == "address2" }?.balance?.cryptoBalance
        )
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

    @Test
    fun `calculate fiat balance with correct values`() {
        val result = MarketUtils.calculateFiatBalance(BigDecimal.TEN, 2.0)
        val expectedBalance = BigDecimal.valueOf(20.00).setScale(2, RoundingMode.HALF_DOWN)
        assertEquals(expectedBalance, result)
    }

    @Test
    fun `calculate fiat balance with empty rate`() {
        val result = MarketUtils.calculateFiatBalance(BigDecimal.TEN, null)
        assertEquals(Double.InvalidValue.toBigDecimal(), result)
    }
}