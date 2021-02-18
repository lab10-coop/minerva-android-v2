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
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.XDAI
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

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
        tokenManager.loadCurrentTokens("Some").size shouldBeEqualTo 0
        tokenManager.loadCurrentTokens(ATS_TAU).let {
            it.size shouldBeEqualTo 4
            it[0].name shouldBeEqualTo "CookieTokenDATS"
            it[1].name shouldBeEqualTo "SomeSomeTokenDATS"
            it[2].name shouldBeEqualTo "CookieTokenATS"
            it[3].name shouldBeEqualTo "OtherTokenATS"
        }
        tokenManager.loadCurrentTokens(ETH_RIN).let {
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
        tokenManager.saveToken(ATS_TAU, firstToken)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveToken(ATS_TAU, firstToken)
            .test()
            .assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test saving tokens list for giving network`() {
        NetworkManager.initialize(DataProvider.networks)
        tokenManager.saveTokens(map)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveTokens(map)
            .test()
            .assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test updating token from local storage`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        val result = tokenManager.updateTokensFromLocalStorage(map)
        result.first shouldBeEqualTo true
        result.second.size shouldBeEqualTo 1
        result.second[ATS_TAU]?.size shouldBeEqualTo 2
        result.second[ATS_TAU]?.get(1)?.balance shouldBeEqualTo BigDecimal.TEN
        result.second[ATS_TAU]?.get(1)?.token?.logoURI shouldBeEqualTo null
        val resultII = tokenManager.updateTokensFromLocalStorage(mapII)
        resultII.first shouldBeEqualTo false
        resultII.second.size shouldBeEqualTo 1
        resultII.second[ETH_RIN]?.size shouldBeEqualTo 2
        resultII.second[ETH_RIN]?.get(1)?.balance shouldBeEqualTo BigDecimal.TEN
        resultII.second[ETH_RIN]?.get(1)?.token?.logoURI shouldBeEqualTo "someLogoURI_II"
    }

    @Test
    fun `Test tokens with online logos data without error`() {
        NetworkManager.initialize(DataProvider.networks)
        val localUpdatedMap = Pair(false, map)
        whenever(cryptoApi.getTokenRawData(any(), any())).thenReturn(Single.just(tokenRawData))
        tokenManager.updateTokens(localUpdatedMap).test().assertComplete().assertNoErrors()
            .assertValue {
                map == map
                map[ATS_TAU]?.get(0)?.token?.logoURI == null
            }
        val localUpdatedMapII = Pair(true, map)
        tokenManager.updateTokens(localUpdatedMapII).test().assertComplete().assertNoErrors()
            .assertValue {
                map[ATS_TAU]?.get(0)?.token?.logoURI == "someIconAddress"
                map[ATS_TAU]?.get(1)?.token?.logoURI == "someIconAddressII"
            }
    }

    @Test
    fun `Test tokens with online logos data with error`() {
        NetworkManager.initialize(DataProvider.networks)
        val localUpdatedMap = Pair(true, map)
        whenever(cryptoApi.getTokenRawData(any(), any())).thenReturn(Single.error(Throwable("No data here!")))
        tokenManager.updateTokens(localUpdatedMap).test().assertErrorMessage("No data here!")
    }

    @Test
    fun `Test saving tokens data`() {
        NetworkManager.initialize(DataProvider.networks)
        val map = mapOf(
            Pair("Some", listOf(AccountToken(firstTokenII, BigDecimal.ONE), AccountToken(secondTokenII, BigDecimal.TEN)))
        )
        tokenManager.saveTokens(map)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveTokens(map)
            .test()
            .assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test updating tokens list`() {
        val newToken = ERC20Token(1, "SomeToken", "some", "0xt0k3n", "32")
        ATS_TAU.let { ATS ->
            val updatedTokens =
                tokenManager.updateTokens(ATS, newToken, DataProvider.walletConfig.erc20Tokens.toMutableMap())
            updatedTokens[ATS]?.size shouldBeEqualTo 3
            updatedTokens[ATS]?.get(2)?.name shouldBeEqualTo "SomeToken"
            updatedTokens[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            val secondNewToken = ERC20Token(1, "CookieCoin", "CC", "0xC00k1e", "32")
            val secondUpdatedToken =
                tokenManager.updateTokens(ATS, secondNewToken, DataProvider.walletConfig.erc20Tokens.toMutableMap())
            secondUpdatedToken[ATS]?.size shouldBeEqualTo 2
            secondUpdatedToken[ATS]?.get(1)?.name shouldBeEqualTo "CookieCoin"
            secondUpdatedToken[ATS]?.get(0)?.name shouldBeEqualTo "OtherTokenATS"
        }
    }

    @Test
    fun `Check mapping from raw addresses to tokens`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        whenever(blockchainRepository.fromGwei(any())).thenReturn(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN)
        val rawTokens = mapOf(
            Pair("0xC00k1e", TokenBalance(balance = "1", name = "CookieTokenATS", address = "0xC00k1e")),
            Pair("0x0th3r2", TokenBalance(balance = "10", name = "CookieTokenOther2ATS", address = "0x0th3r2"))
        )
        val tokensATS = tokenManager.mapToAccountTokensList(ATS_SIGMA, rawTokens)
        tokensATS.size shouldBeEqualTo 4
        tokensATS[0].token.name shouldBeEqualTo "CookieTokenOther2ATS"
        tokensATS[0].balance shouldBeEqualTo BigDecimal.TEN
        tokensATS[1].token.name shouldBeEqualTo "CookieTokenATS"
        tokensATS[1].balance shouldBeEqualTo BigDecimal.ONE
        tokensATS[2].token.name shouldBeEqualTo "SecondOtherATS"
        tokensATS[2].balance shouldBeEqualTo BigDecimal.ZERO
        tokensATS[3].token.name shouldBeEqualTo "OtherTokenATS"
        tokensATS[3].balance shouldBeEqualTo BigDecimal.ZERO

        val tokenETH = tokenManager.mapToAccountTokensList(ETH_RIN, rawTokens)
        tokenETH.size shouldBeEqualTo 4
        tokenETH[0].token.name shouldBeEqualTo "CookieTokenOther2ATS"
        tokenETH[0].balance shouldBeEqualTo BigDecimal.TEN
        tokenETH[1].token.name shouldBeEqualTo "CookieTokenATS"
        tokenETH[1].balance shouldBeEqualTo BigDecimal.ONE
        tokenETH[2].token.name shouldBeEqualTo "OtherTokenDETH"
        tokenETH[2].balance shouldBeEqualTo BigDecimal.ZERO
        tokenETH[3].token.name shouldBeEqualTo "OtherTokenETH"
        tokenETH[3].balance shouldBeEqualTo BigDecimal.ZERO
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

    @Test
    fun `Creating correct token URLs`() {
        val accountOne = Account(1, networkShort = ATS_TAU, address = "0xADDRESSxONE")
        val accountTwo = Account(1, networkShort = ETH_ROP, address = "0xADDRESSxTWO")
        val accountThree = Account(1, networkShort = POA_SKL, address = "0xADDRESSxTHREE")
        val accountFour = Account(1, networkShort = LUKSO_14, address = "0xADDRESSxFOUR")
        val accountFive = Account(1, networkShort = ATS_SIGMA, address = "0xADDRESSxFIVE")
        val accountSix = Account(1, networkShort = XDAI, address = "0xADDRESSxSIX")
        val accountSeven = Account(1, networkShort = POA_CORE, address = "0xADDRESSxSEVEN")
        val accountEight = Account(1, networkShort = "empty", address = "0xADDRESSxEMPTY")

        tokenManager.getTokensApiURL(accountOne) shouldBeEqualTo "https://explorer.tau1.artis.network/api?module=account&action=tokenlist&address=0xADDRESSxONE"
        tokenManager.getTokensApiURL(accountTwo) shouldBeEqualTo "https://explorer.tau1.artis.network/api?module=account&action=tokenlist&address=0xADDRESSxTWO"
        tokenManager.getTokensApiURL(accountThree) shouldBeEqualTo "https://blockscout.com/poa/sokol/api?module=account&action=tokenlist&address=0xADDRESSxTHREE"
        tokenManager.getTokensApiURL(accountFour) shouldBeEqualTo "https://blockscout.com/lukso/l14/api?module=account&action=tokenlist&address=0xADDRESSxFOUR"
        tokenManager.getTokensApiURL(accountFive) shouldBeEqualTo "https://explorer.sigma1.artis.network/api?module=account&action=tokenlist&address=0xADDRESSxFIVE"
        tokenManager.getTokensApiURL(accountSix) shouldBeEqualTo "https://blockscout.com/poa/xdai/api?module=account&action=tokenlist&address=0xADDRESSxSIX"
        tokenManager.getTokensApiURL(accountSeven) shouldBeEqualTo "https://blockscout.com/poa/core/api?module=account&action=tokenlist&address=0xADDRESSxSEVEN"
        assertFailsWith<NetworkNotFoundThrowable> { tokenManager.getTokensApiURL(accountEight) }
    }

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills


    private val data = listOf(
        TokenIconDetails(1, "0x4ddre55", "LogoUri"),
        TokenIconDetails(2, "0x4ddre55", "LogoUri2")
    )

    private val firstToken = ERC20Token(1, "CookieToken", "COOKiE", "0xC00k1e", "C01")
    private val secondToken = ERC20Token(1, "CookieTokenII", "COOKiE", "0xC00k1eII", "C02")
    private val map = mapOf(
        Pair(
            ATS_TAU,
            listOf(AccountToken(firstToken, BigDecimal.ONE), AccountToken(secondToken, BigDecimal.TEN))
        )
    )

    private val firstTokenII = ERC20Token(2, "CookieTokenRIN", "COOKiERIN", "0x0th3r", "C11")
    private val secondTokenII = ERC20Token(2, "CookieTokenRINII", "COOKiERINII", "0xC00k1e", "C12")
    private val mapII = mapOf(
        Pair(
            ETH_RIN,
            listOf(AccountToken(firstTokenII, BigDecimal.ONE), AccountToken(secondTokenII, BigDecimal.TEN))
        )
    )

    private val tokenRawData = listOf(
        TokenIconDetails(1, "0xC00k1e", "someIconAddress"),
        TokenIconDetails(1, "0xC00k1eII", "someIconAddressII")
    )
}