package minerva.android.walletmanager.manager.accounts.tokens

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.*
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TokenManagerTest : RxTest() {

    private val walletManager: WalletConfigManager = mock()
    private val cryptoApi: CryptoApi = mock()
    private val localStorage: LocalStorage = mock()
    private val blockchainRepository: BlockchainRegularAccountRepository = mock()
    private val tokenManager = TokenManagerImpl(walletManager, cryptoApi, localStorage, blockchainRepository)

    @Before
    fun initializeMocks() {
        whenever(walletManager.getWalletConfig()).thenReturn(null, DataProvider.walletConfig, DataProvider.walletConfig)
        whenever(walletManager.updateWalletConfig(any())).thenReturn(Completable.complete())
    }

    @Test
    fun `Test loading tokens list`() {
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
        val firstToken = ERC20Token(1, "CookieToken", "COOKiE", "0xC00k1e", "C00")
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
        val newToken = ERC20Token(1, "SomeToken", "some", "0xt0k3n", "32")
        NetworkShortName.ATS_TAU.let { ATS ->
            val updatedTokens =
                tokenManager.updateTokens(ATS, newToken, DataProvider.walletConfig.erc20Tokens.toMutableMap())
            updatedTokens[ATS]?.size shouldBeEqualTo 3
            updatedTokens[ATS]?.get(2)?.name shouldBeEqualTo "SomeToken"
            updatedTokens[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            val secondNewToken = ERC20Token(1, "CookieCoin", "CC", "0xC00k1e", "32")
            val secondUpdatedToken = tokenManager.updateTokens(ATS, secondNewToken, DataProvider.walletConfig.erc20Tokens.toMutableMap())
            secondUpdatedToken[ATS]?.size shouldBeEqualTo 2
            secondUpdatedToken[ATS]?.get(1)?.name shouldBeEqualTo "CookieCoin"
            secondUpdatedToken[ATS]?.get(0)?.name shouldBeEqualTo "OtherTokenATS"
        }
    }

    @Test
    fun `Check mapping from raw addresses to tokens`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        whenever(blockchainRepository.fromGwei(any())).thenReturn(BigDecimal.ONE)
        val rawTokens = listOf(
            Pair("0xC00k1e", TokenBalance(balance = "1")),
            Pair("0x0th3r", TokenBalance(balance = "10"))
        )
        val tokensATS = tokenManager.mapToAccountTokensList(NetworkShortName.ATS_TAU, rawTokens)
        tokensATS.size shouldBeEqualTo 2
        tokensATS[0].token.name shouldBeEqualTo "CookieTokenATS"
        tokensATS[1].token.name shouldBeEqualTo "OtherTokenATS"
        val tokenETH = tokenManager.mapToAccountTokensList(NetworkShortName.ETH_RIN, rawTokens)
        tokenETH.size shouldBeEqualTo 2
        tokenETH[0].token.name shouldBeEqualTo "CookieTokenDETH"
        tokenETH[1].token.name shouldBeEqualTo "OtherTokenETH"
    }

    @Test
    fun `Check getting Token Icon URL method`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenRawData(any(), any())).thenReturn(Single.just(data), Single.just(listOf()))
        tokenManager.getTokenIconURL(1, "0x4ddre55")
            .test()
            .assertComplete()
            .values().let {
                it.first() shouldBeEqualTo "LogoUri"
            }
        tokenManager.getTokenIconURL(1, "0x4ddre55")
            .test()
            .assertComplete()
            .values().let {
                it.first() shouldBeEqualTo ""
            }

        verify(cryptoApi, times(2)).getTokenRawData(any(), any())
    }

    @Test
    fun `Check that generating key for map is correct`() {
        val chaiId = 3
        val address = "0x4ddr355"
        val key = tokenManager.generateTokenIconKey(chaiId, address)
        key shouldBeEqualTo "30x4ddr355"
    }

    @Test
    fun `Check mapping last commit data to last commit timestamp`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenLastCommitRawData(any(), any())).thenReturn(Single.just(commitData))
        whenever(cryptoApi.getTokenRawData(any(), any())).thenReturn(Single.just(data))
        whenever(localStorage.loadTokenIconsUpdateTimestamp()).thenReturn(333L, 1611950162000, 1611950162333)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        tokenManager.updateTokenIcons().test().assertComplete()
        tokenManager.updateTokenIcons().test().assertNotComplete()
        tokenManager.updateTokenIcons().test().assertNotComplete()
    }

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills


    private val data = listOf(
        TokenIconDetails(1, "0x4ddre55", "LogoUri"),
        TokenIconDetails(2, "0x4ddre55", "LogoUri2")
    )
}