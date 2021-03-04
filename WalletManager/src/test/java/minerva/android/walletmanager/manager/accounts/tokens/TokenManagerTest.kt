package minerva.android.walletmanager.manager.accounts.tokens

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.*
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.wallet.WalletConfig
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
        tokenManager.loadCurrentTokens(246785).size shouldBeEqualTo 0
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
        tokenManager.saveTokens(true, map)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveTokens(true, map)
            .test()
            .assertComplete()
        tokenManager.saveTokens(false, map)
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
        result.second["ATS_TAU"]?.size shouldBeEqualTo 2
        result.second["ATS_TAU"]?.get(1)?.balance?.toPlainString() shouldBeEqualTo "0.1"
        result.second["ATS_TAU"]?.get(1)?.token?.logoURI shouldBeEqualTo null
        val resultII = tokenManager.updateTokensFromLocalStorage(mapII)
        resultII.first shouldBeEqualTo false
        resultII.second.size shouldBeEqualTo 1
        resultII.second["ETH_RIN"]?.size shouldBeEqualTo 2
        resultII.second["ETH_RIN"]?.get(1)?.balance?.toPlainString() shouldBeEqualTo "0.1"
        resultII.second["ETH_RIN"]?.get(1)?.token?.logoURI shouldBeEqualTo "someLogoURI_II"
        val resultIII = tokenManager.updateTokensFromLocalStorage(mapIII)
        resultIII.first shouldBeEqualTo true
        resultIII.second.size shouldBeEqualTo 1
        resultIII.second["ETH_RIN"]?.size shouldBeEqualTo 3
        resultIII.second["ETH_RIN"]?.get(1)?.token?.logoURI shouldBeEqualTo "someLogoURI_II"
        resultIII.second["ETH_RIN"]?.get(2)?.token?.logoURI shouldBeEqualTo null
    }

    @Test
    fun `Test tokens with online logos data without error`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenRawData(any())).thenReturn(Single.just(tokenRawData))
        tokenManager.updateTokenIcons(false, map).test().assertComplete().assertNoErrors()
            .assertValue {
                map == map
                map["ATS_TAU"]?.get(0)?.token?.logoURI == null
            }
        tokenManager.updateTokenIcons(true, map).test().assertComplete().assertNoErrors()
            .assertValue {
                map["ATS_TAU"]?.get(0)?.token?.logoURI == "someIconAddress"
                map["ATS_TAU"]?.get(1)?.token?.logoURI == "someIconAddressII"
            }
    }

    @Test
    fun `Test tokens with online logos data with error`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenRawData(any())).thenReturn(Single.error(Throwable("No data here!")))
        tokenManager.updateTokenIcons(true, map).test().assertErrorMessage("No data here!")
    }

    @Test
    fun `Test saving tokens data`() {
        NetworkManager.initialize(DataProvider.networks)
        val map = mapOf(
            Pair("Some", listOf(AccountToken(firstTokenII, BigDecimal.ONE), AccountToken(secondTokenII, BigDecimal.TEN)))
        )
        tokenManager.saveTokens(true, map)
            .test()
            .assertErrorMessage(NotInitializedWalletConfigThrowable().message)
        tokenManager.saveTokens(true, map)
            .test()
            .assertComplete()
        tokenManager.saveTokens(false, map)
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
        val rawTokens = listOf(
            AccountToken(
                ERC20Token(chainId = 0, name = "CookieTokenATS", address = "0xC00k1e", decimals = "2"),
                10000.toBigDecimal()
            ),
            AccountToken(
                ERC20Token(chainId = 0, name = "CookieTokenOther2ATS", address = "0x0th3r2", decimals = "3"),
                1000000000.toBigDecimal()
            )
        )

        val tokensATS = tokenManager.prepareCurrentTokenList(ATS_SIGMA, rawTokens)
        tokensATS.size shouldBeEqualTo 4
        tokensATS[0].token.name shouldBeEqualTo "CookieTokenOther2ATS"
        tokensATS[0].balance.toPlainString() shouldBeEqualTo "1000000"
        tokensATS[1].token.name shouldBeEqualTo "CookieTokenATS"
        tokensATS[1].balance.toPlainString() shouldBeEqualTo "100"
        tokensATS[2].token.name shouldBeEqualTo "SecondOtherATS"
        tokensATS[2].balance.toPlainString() shouldBeEqualTo "0"
        tokensATS[3].token.name shouldBeEqualTo "OtherTokenATS"
        tokensATS[3].balance shouldBeEqualTo BigDecimal.ZERO

        val tokenETH = tokenManager.prepareCurrentTokenList(ETH_RIN, rawTokens)
        tokenETH.size shouldBeEqualTo 4
        tokenETH[0].token.name shouldBeEqualTo "CookieTokenOther2ATS"
        tokenETH[0].balance.toPlainString() shouldBeEqualTo "1000000"
        tokenETH[1].token.name shouldBeEqualTo "CookieTokenATS"
        tokenETH[1].balance.toPlainString() shouldBeEqualTo "100"
        tokenETH[2].token.name shouldBeEqualTo "OtherTokenDETH"
        tokenETH[2].balance.toPlainString() shouldBeEqualTo "0"
        tokenETH[3].token.name shouldBeEqualTo "OtherTokenETH"
        tokenETH[3].balance.toPlainString() shouldBeEqualTo "0"
    }

    @Test
    fun `Check getting Token Icon URL method`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenRawData(any())).thenReturn(Single.just(data), Single.just(listOf()))
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

        verify(cryptoApi, times(2)).getTokenRawData(any())
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
        whenever(cryptoApi.getTokenLastCommitRawData(any())).thenReturn(Single.just(commitData))
        whenever(cryptoApi.getTokenRawData(any())).thenReturn(Single.just(data))
        whenever(localStorage.loadTokenIconsUpdateTimestamp()).thenReturn(333L, 1611950162000, 1611950162333)
        whenever(walletManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        tokenManager.updateTokenIcons().test().assertComplete()
        tokenManager.updateTokenIcons().test().assertNotComplete()
        tokenManager.updateTokenIcons().test().assertNotComplete()
    }

    @Test
    fun `Creating correct token URLs`() {
        val accountOne = Account(1, chainId = ATS_TAU, address = "0xADDRESSxONE")
        val accountTwo = Account(1, chainId = ETH_ROP, address = "0xADDRESSxTWO")
        val accountThree = Account(1, chainId = POA_SKL, address = "0xADDRESSxTHREE")
        val accountFour = Account(1, chainId = LUKSO_14, address = "0xADDRESSxFOUR")
        val accountFive = Account(1, chainId = ATS_SIGMA, address = "0xADDRESSxFIVE")
        val accountSix = Account(1, chainId = XDAI, address = "0xADDRESSxSIX")
        val accountSeven = Account(1, chainId = POA_CORE, address = "0xADDRESSxSEVEN")
        val accountEight = Account(1, chainId = -1, address = "0xADDRESSxEMPTY")

        tokenManager.getTokensApiURL(accountOne) shouldBeEqualTo "https://explorer.tau1.artis.network/api?module=account&action=tokenlist&address=0xADDRESSxONE"
        tokenManager.getTokensApiURL(accountTwo) shouldBeEqualTo "https://api-ropsten.etherscan.io/api?module=account&action=tokenlist&address=0xADDRESSxTWO"
        tokenManager.getTokensApiURL(accountThree) shouldBeEqualTo "https://blockscout.com/poa/sokol/api?module=account&action=tokenlist&address=0xADDRESSxTHREE"
        tokenManager.getTokensApiURL(accountFour) shouldBeEqualTo "https://blockscout.com/lukso/l14/api?module=account&action=tokenlist&address=0xADDRESSxFOUR"
        tokenManager.getTokensApiURL(accountFive) shouldBeEqualTo "https://explorer.sigma1.artis.network/api?module=account&action=tokenlist&address=0xADDRESSxFIVE"
        tokenManager.getTokensApiURL(accountSix) shouldBeEqualTo "https://blockscout.com/poa/xdai/api?module=account&action=tokenlist&address=0xADDRESSxSIX"
        tokenManager.getTokensApiURL(accountSeven) shouldBeEqualTo "https://blockscout.com/poa/core/api?module=account&action=tokenlist&address=0xADDRESSxSEVEN"
        assertFailsWith<NetworkNotFoundThrowable> { tokenManager.getTokensApiURL(accountEight) }
    }

    @Test
    fun `Test refreshing token balance`() {
        val notEtherscanAccount = Account(1, chainId = ATS_TAU, address = "0xADDRESSxONE")
        val etherscanAccount = Account(1, chainId = ETH_RIN, address = "0xADDRESSxTWO")
        val tokenBalances = listOf(
            TokenBalance("t01", "s01", "name01", "2", "0xC00KiE01", "1000"),
            TokenBalance("t02", "s02", "name02", "3", "0xC00KiE02", "100000")
        )
        val tokenResponse = TokenBalanceResponse("OK", tokenBalances, "response N O W !")

        val tokenTXs = listOf(
            TokenTx(tokenName = "name03", address = "0xC00KiE03", tokenDecimal = "3"),
            TokenTx(tokenName = "name04", address = "0xC00KiE04", tokenDecimal = "6")
        )

        val tokenTxResponse = TokenTxResponse("OK", tokenTXs, "response N O W !")
        val refreshTokenResponse01 = Observable.just(Pair("0xC00KiE03", 10000.toBigDecimal()))
        val refreshTokenResponse02 = Observable.just(Pair("0xC00KiE04", 100000000.toBigDecimal()))

        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptoApi.getTokenBalance(any())).thenReturn(Single.just(tokenResponse))
        whenever(blockchainRepository.refreshTokenBalance(any(), any(), any(), any())).thenReturn(
            refreshTokenResponse01,
            refreshTokenResponse02
        )
        whenever(cryptoApi.getTokenTx(any())).thenReturn(Single.just(tokenTxResponse))
        tokenManager.refreshTokenBalance(notEtherscanAccount)
            .test()
            .assertComplete()
            .assertValue {
                it.size == 2
                it[0].token.name == "name01"
                it[0].balance.toPlainString() == "10"
                it[1].token.name == "name02"
                it[1].balance.toPlainString() == "100"
            }
        tokenManager.refreshTokenBalance(etherscanAccount)
            .test()
            .assertComplete()
            .assertValue {
                it.size == 2
                it[0].token.name == "name03"
                it[0].balance.toPlainString() == "10"
                it[1].token.name == "name04"
                it[1].balance.toPlainString() == "100"
            }

    }

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills


    private val data = listOf(
        TokenIconDetails(1, "0x4ddre55", "LogoUri"),
        TokenIconDetails(2, "0x4ddre55", "LogoUri2")
    )

    private val firstToken = ERC20Token(ATS_TAU, "CookieToken", "COOKiE", "0xC00k1e", "1")
    private val secondToken = ERC20Token(ATS_TAU, "CookieTokenII", "COOKiE", "0xC00k1eII", "2")
    private val map = mapOf(
        Pair(
            "ATS_TAU",
            listOf(
                AccountToken(firstToken, BigDecimal.ONE),
                AccountToken(secondToken, BigDecimal.TEN))
        )
    )

    private val firstTokenII = ERC20Token(ETH_RIN, "CookieTokenRIN", "COOKiERIN", "0x0th3r", "1")
    private val secondTokenII = ERC20Token(ETH_RIN, "CookieTokenRINII", "COOKiERINII", "0xC00k1e", "2")

    private val mapII = mapOf(
        Pair(
            "ETH_RIN",
            listOf(AccountToken(firstTokenII, BigDecimal.ONE), AccountToken(secondTokenII, BigDecimal.TEN))
        )
    )

    private val firstTokenIII = ERC20Token(ETH_RIN, "CookieTokenTINIII", "COOKiERINIII", "0x000000", "3")

    private val mapIII = mapOf(
        Pair(
            "ETH_RIN",
            listOf(AccountToken(firstTokenII, BigDecimal.ONE), AccountToken(secondTokenII, BigDecimal.TEN), AccountToken(firstTokenIII, BigDecimal.ZERO))
        )
    )

    private val tokenRawData = listOf(
        TokenIconDetails(ATS_TAU, "0xC00k1e", "someIconAddress"),
        TokenIconDetails(ATS_TAU, "0xC00k1eII", "someIconAddressII")
    )
}