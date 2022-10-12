package minerva.android.walletmanager.utils

import minerva.android.apiProvider.model.FiatPrice
import minerva.android.apiProvider.model.Markets
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinCryptoBalance
import minerva.android.walletmanager.model.network.Network
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals

class MarketUtilsTest {

    private val poaNetwork = Network(testNet = false, chainId = POA_CORE, httpRpc = "some")
    private val xdaiNetwork = Network(testNet = false, chainId = GNO, httpRpc = "some")

    private val xdaiTestNetwork = Network(testNet = true, chainId = GNO)
    private val markets = Markets(poaFiatPrice = FiatPrice(eur = 0.5), daiFiatPrice = FiatPrice(2.0))

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets enabled`() {
        NetworkManager.initialize(listOf(poaNetwork, xdaiNetwork))
        val coinCryptoBalance = CoinCryptoBalance(poaNetwork.chainId, "address1", BigDecimal(12))
        val result = MarketUtils.calculateFiatBalance(coinCryptoBalance, markets, "EUR")
        val expectedFiatBalance = BigDecimal.valueOf(6.00).setScale(2, RoundingMode.HALF_DOWN)

        result.chainId shouldBeEqualTo poaNetwork.chainId
        result.balance.fiatBalance shouldBeEqualTo expectedFiatBalance
        result.balance.cryptoBalance shouldBeEqualTo BigDecimal(12)
        result.address shouldBeEqualTo "address1"
    }

    @Test
    fun `calculate fiat balance in euro for xdai and poa when main nets disabled`() {
        val coinCryptoBalance = CoinCryptoBalance(xdaiTestNetwork.chainId, "address2", BigDecimal(10))
        val result = MarketUtils.calculateFiatBalance(coinCryptoBalance, markets, "EUR")
        val expectedFiatBalance = BigDecimal.valueOf(20.00).setScale(2, RoundingMode.HALF_DOWN)

        result.chainId shouldBeEqualTo xdaiNetwork.chainId
        result.balance.fiatBalance shouldBeEqualTo expectedFiatBalance
        result.balance.cryptoBalance shouldBeEqualTo BigDecimal(10)
        result.address shouldBeEqualTo "address2"
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
        val result1 = MarketUtils.getCoinGeckoMarketId(accounts[0].chainId)
        val result2 = MarketUtils.getCoinGeckoMarketId(accounts[1].chainId)
        val result3 = MarketUtils.getCoinGeckoMarketId(accounts[2].chainId)

        result1 shouldBeEqualTo "ethereum"
        result2 shouldBeEqualTo "poa-network"
        result3 shouldBeEqualTo "xdai"
    }

    @Test
    fun `get token market ids test`() {
        NetworkManager.initialize(
            listOf(
                Network(chainId = 1, httpRpc = "some"),
                Network(chainId = 42220, httpRpc = "some"),
                Network(chainId = 100, httpRpc = "some")
            )
        )
        val accounts =
            listOf(
                Account(1, chainId = 1),
                Account(2, chainId = 42220),
                Account(3, chainId = 137)
            )
        val result1 = MarketUtils.getTokenGeckoMarketId(accounts[0].chainId)
        val result2 = MarketUtils.getTokenGeckoMarketId(accounts[1].chainId)
        val result3 = MarketUtils.getTokenGeckoMarketId(accounts[2].chainId)

        result1 shouldBeEqualTo "ethereum"
        result2 shouldBeEqualTo "celo"
        result3 shouldBeEqualTo "polygon-pos"
    }

    @Test
    fun `get market ids with no accounts test`() {
        val result = MarketUtils.getCoinGeckoMarketId(123434)
        assertEquals("", result)
    }

    @Test
    fun `get rate test`() {
        val markets =
            listOf(
                Markets(ethFiatPrice = FiatPrice(eur = 1.0)),
                Markets(poaFiatPrice = FiatPrice(gbp = 3.0)),
                Markets(maticFiatPrice = FiatPrice(pln = 2.0))
            )

        val result1 = MarketUtils.getRate(1, markets[0], "EUR")
        val result2 = MarketUtils.getRate(99, markets[1], "GBP")
        val result3 = MarketUtils.getRate(137, markets[2], "PLN")

        result1 shouldBeEqualTo 1.0
        result2 shouldBeEqualTo 3.0
        result3 shouldBeEqualTo 2.0
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