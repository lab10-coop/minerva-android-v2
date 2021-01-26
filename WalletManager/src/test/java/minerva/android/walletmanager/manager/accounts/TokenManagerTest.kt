package minerva.android.walletmanager.manager.accounts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.utils.RxTest
import minerva.android.walletmanager.manager.accounts.tokens.TokenManagerImpl
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TokenManagerTest : RxTest() {

    private val walletManager: WalletConfigManager = mock()
    private val tokenManager = TokenManagerImpl(walletManager)

    @Before
    fun initializeMocks() {
        whenever(walletManager.getWalletConfig()).thenReturn(null, DataProvider.walletConfig, DataProvider.walletConfig)
        whenever(walletManager.updateWalletConfig(any())).thenReturn(Completable.complete())
    }

    @Test
    fun `Test loading tokens list` () {
        NetworkManager.initialize(DataProvider.networks)
        tokenManager.loadTokens("Some").size shouldBeEqualTo 0
        tokenManager.loadTokens(NetworkShortName.ATS_TAU).let {
            it.size shouldBeEqualTo 4
            it[0].name shouldBeEqualTo "CookieTokenATS"
            it[1].name shouldBeEqualTo "CookieTokenDATS"
            it[2].name shouldBeEqualTo "OtherTokenATS"
            it[3].name shouldBeEqualTo "SomeSomeTokenDATS"
        }
        tokenManager.loadTokens(NetworkShortName.ETH_RIN).let {
            it.size shouldBeEqualTo 3
            it[0].name shouldBeEqualTo "CookieTokenDETH"
            it[1].name shouldBeEqualTo "OtherTokenDETH"
            it[2].name shouldBeEqualTo "OtherTokenETH"
        }
    }

    @Test
    fun `Test saving tokens for giving network`() {
        NetworkManager.initialize(DataProvider.networks)
        val firstToken = Token("CookieToken", "COOKiE", "0xC00k1e", "C00")
        tokenManager.saveToken(NetworkShortName.ATS_TAU, firstToken)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveToken(NetworkShortName.ATS_TAU, firstToken)
            .test()
            .assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test updating tokens list`() {
        val newToken = Token("SomeToken", "some", "0xt0k3n", "32")
        NetworkShortName.ATS_TAU.let { ATS ->
            val updatedTokens =
                tokenManager.updateTokens(ATS, newToken, DataProvider.walletConfig.erc20Tokens)
            updatedTokens[ATS]?.size shouldBeEqualTo 3
            updatedTokens[ATS]?.get(2)?.name shouldBeEqualTo "SomeToken"
            updatedTokens[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            val secondNewToken = Token("CookieCoin", "CC", "0xC00k1e", "32")
            val secondUpdatedToken = tokenManager.updateTokens(ATS, secondNewToken, DataProvider.walletConfig.erc20Tokens)
            secondUpdatedToken[ATS]?.size shouldBeEqualTo 2
            secondUpdatedToken[ATS]?.get(1)?.name shouldBeEqualTo "CookieCoin"
            secondUpdatedToken[ATS]?.get(0)?.name shouldBeEqualTo "OtherTokenATS"
        }
    }

    @Test
    fun `Check mapping from raw addresses to tokens` () {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        val rawTokens = listOf(
            Pair("0xC00k1e", BigDecimal.ONE),
            Pair("0x0th3r", BigDecimal.TEN)
        )
        val tokensATS = tokenManager.mapToAccountTokensList(NetworkShortName.ATS_TAU, rawTokens)
        tokensATS.size shouldBeEqualTo 2
        tokensATS[0].token.name shouldBeEqualTo "CookieTokenATS"
        tokensATS[1].token.name shouldBeEqualTo "OtherTokenATS"
        val tokenETH = tokenManager.mapToAccountTokensList(NetworkShortName.ETH_RIN, rawTokens)
        tokenETH.size shouldBeEqualTo 2
        tokenETH[0].token.name shouldBeEqualTo  "CookieTokenDETH"
        tokenETH[1].token.name shouldBeEqualTo  "OtherTokenETH"
    }
}