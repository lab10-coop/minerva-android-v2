package minerva.android.walletmanager.manager.accounts.tokens

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.*
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.superToken.SuperTokenRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.database.dao.TokenDao
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.AssetBalance
import minerva.android.walletmanager.model.minervaprimitives.account.AssetError
import minerva.android.walletmanager.model.token.*
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.utils.MockDataProvider
import minerva.android.walletmanager.utils.RxTest
import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFailsWith

class TokenManagerTest : RxTest() {

    private val walletManager: WalletConfigManager = mock()
    private val cryptoApi: CryptoApi = mock()
    private val localStorage: LocalStorage = mock()
    private val superTokenRepository: SuperTokenRepository = mock()
    private val erc20TokenRepository: ERC20TokenRepository = mock()
    private val erc721TokenRepository: ERC721TokenRepository = mock()
    private val erc1155TokenRepository: ERC1155TokenRepository = mock()
    private val rateStorage: RateStorage = mock()
    private val tokenDao: TokenDao = mock()
    private lateinit var database: MinervaDatabase
    private lateinit var tokenManager: TokenManagerImpl

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills

    private val firstToken = ERCToken(ATS_TAU, "CookieToken", "COOKiE", "0xC00k1e", "1", type = TokenType.ERC20)
    private val secondToken = ERCToken(ATS_TAU, "CookieTokenII", "COOKiE", "0xC00k1eII", "2", type = TokenType.ERC20)
    private val map = mapOf(Pair(1, listOf(firstToken, secondToken)))

    private val firstTokenII = ERCToken(ETH_RIN, "CookieTokenRIN", "COOKiERIN", "0x0th3r", "1", type = TokenType.ERC20)
    private val secondTokenII = ERCToken(ETH_RIN, "CookieTokenRINII", "COOKiERINII", "0xC00k1e", "2", type = TokenType.ERC20)

    private val tokenRawData = listOf(
        TokenDetails(ATS_TAU, "0xC00k1e", "someIconAddress"),
        TokenDetails(ATS_TAU, "0xC00k1eII", "someIconAddressII")
    )

    @Before
    fun initializeMocks() {
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig, MockDataProvider.walletConfig)
        whenever(walletManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        database = mock { whenever(mock.tokenDao()).thenReturn(tokenDao) }
        tokenManager =
            TokenManagerImpl(
                walletManager,
                cryptoApi,
                localStorage,
                superTokenRepository,
                erc20TokenRepository,
                erc721TokenRepository,
                erc1155TokenRepository,
                rateStorage
            )
    }

    @Test
    fun `Test loading tokens list`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)

        tokenManager.getActiveTokensPerAccount(Account(1, chainId = ETH_RIN, address = "address123")).let {
            it.size shouldBeEqualTo 2
            it[0].name shouldBeEqualTo "OtherTokenETH"
            it[1].name shouldBeEqualTo "CookieTokenETH"
        }

        tokenManager.getActiveTokensPerAccount(Account(2, chainId = ATS_SIGMA, address = "0xADDRESSxTWO")).let {
            it.size shouldBeEqualTo 3
            it[0].name shouldBeEqualTo "CookieTokenATS"
            it[1].name shouldBeEqualTo "SecondOtherATS"
            it[2].name shouldBeEqualTo "OtherTokenATS"
        }
    }

    @Test
    fun `Test saving tokens for given network`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(tokenDao.getTaggedTokens()).thenReturn(
            Single.just(
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "CookieTokenATS",
                        "Cookie",
                        "0xS0m3T0k3N",
                        "13",
                        type = TokenType.ERC20
                    )
                )
            )
        )
        val firstToken = ERCToken(1, "CookieToken", "COOKiE", "0xC00k1e", "C00", type = TokenType.ERC20)
        tokenManager.saveToken("accountAddress", ATS_TAU, firstToken).test().assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test saving tokens list for giving network`() {
        NetworkManager.initialize(MockDataProvider.networks)
        tokenManager.saveTokens(true, map)
            .test()
            .assertComplete()
        tokenManager.saveTokens(false, map)
            .test()
            .assertComplete()
        verify(walletManager, times(1)).updateWalletConfig(any())
    }

    @Test
    fun `Test saving tokens data`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val map = mapOf(
            Pair(1, listOf(firstTokenII, secondTokenII))
        )
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
        val newToken = ERCToken(1, "SomeToken", "some", "0xt0k3n", "32", accountAddress = "address1", type = TokenType.ERC20)

        ATS_TAU.let { ATS ->
            val updatedTokens =
                tokenManager.updateTokens(ATS, newToken, MockDataProvider.walletConfig.erc20Tokens.toMutableMap())
            updatedTokens[ATS]?.size shouldBeEqualTo 5
            updatedTokens[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            updatedTokens[ATS]?.get(1)?.name shouldBeEqualTo "OtherTokenATS1"
            updatedTokens[ATS]?.get(2)?.name shouldBeEqualTo "OtherTokenATS"
            updatedTokens[ATS]?.get(3)?.name shouldBeEqualTo "TokenTest1"
            updatedTokens[ATS]?.get(4)?.name shouldBeEqualTo "SomeToken"

            val secondNewToken = ERCToken(1, "CookieCoin", "CC", "0xC00k1e", "32", type = TokenType.ERC20)
            val secondUpdatedToken =
                tokenManager.updateTokens(1, secondNewToken, MockDataProvider.walletConfig.erc20Tokens.toMutableMap())
            secondUpdatedToken[1]?.size shouldBeEqualTo 1
            secondUpdatedToken[1]?.get(0)?.name shouldBeEqualTo "CookieCoin"
            secondUpdatedToken[ATS]?.size shouldBeEqualTo 4
            secondUpdatedToken[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            secondUpdatedToken[ATS]?.get(1)?.name shouldBeEqualTo "OtherTokenATS1"
            secondUpdatedToken[ATS]?.get(2)?.name shouldBeEqualTo "OtherTokenATS"
            secondUpdatedToken[ATS]?.get(3)?.name shouldBeEqualTo "TokenTest1"
        }
    }

    @Test
    fun `Test updating tokens list with tokens map`() {
        val newTokens = mapOf(
            Pair(
                ATS_TAU, listOf(
                    ERCToken(
                        ATS_TAU,
                        "SomeToken01",
                        "some01",
                        "address1",
                        "32",
                        accountAddress = "accountADDress1",
                        logoURI = "sd",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "SomeToken01",
                        "some01",
                        "address1",
                        "32",
                        accountAddress = "accountAddress1",
                        logoURI = "sd",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "SomeToken02",
                        "some01",
                        "address2",
                        "16",
                        accountAddress = "accountAddress1",
                        type = TokenType.ERC20
                    ),
                    ERCToken(
                        ATS_TAU,
                        "SomeToken03",
                        "some02",
                        "address3",
                        "16",
                        accountAddress = "accountAddress2",
                        type = TokenType.ERC20
                    )
                )
            ),
            Pair(
                ETH_RIN, listOf(
                    ERCToken(ETH_RIN, "SomeToken03", "some03", "0xt0k3n03", "32", type = TokenType.ERC20),
                    ERCToken(ETH_RIN, "SomeToken04", "some04", "0xC00k1e", "16", type = TokenType.ERC20),
                    ERCToken(ETH_RIN, "SomeToken05", "some05", "ad1", "12", type = TokenType.ERC20),
                    ERCToken(ETH_RIN, "SomeToken05", "some05", "ad1", "12", type = TokenType.ERC20),
                    ERCToken(ETH_RIN, "SomeToken05", "some05", "ad1", "12", type = TokenType.ERC20)
                )
            )
        )

        val result = "accountADDress1".toLowerCase()
        result shouldBeEqualTo "accountaddress1"

        val updatedTokens =
            tokenManager.updateTokens(newTokens)
        updatedTokens[ATS_TAU]?.size shouldBeEqualTo 3
        updatedTokens[ATS_TAU]?.get(0)?.name shouldBeEqualTo "SomeToken01"
        updatedTokens[ATS_TAU]?.get(1)?.name shouldBeEqualTo "SomeToken02"
        updatedTokens[ATS_TAU]?.get(2)?.name shouldBeEqualTo "SomeToken03"

        updatedTokens[ETH_RIN]?.size shouldBeEqualTo 3
        updatedTokens[ETH_RIN]?.get(0)?.name shouldBeEqualTo "SomeToken03"
        updatedTokens[ETH_RIN]?.get(1)?.name shouldBeEqualTo "SomeToken04"
        updatedTokens[ETH_RIN]?.get(2)?.name shouldBeEqualTo "SomeToken05"
    }

    @Test
    fun `Check sorting ERC20Token list into map`() {
        val tokens = listOf(
            ERCToken(1, "chainOne", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(2, "chainTwo", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(2, "chainTwoTwo", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(3, "chainThree", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(3, "chainThreeThree", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(3, "chainOneThreeThreeThree", address = "0x0N3", type = TokenType.ERC20)
        )

        val resultMap = tokenManager.sortTokensByChainId(tokens)
        resultMap.size shouldBeEqualTo 3
        resultMap[1]?.size shouldBeEqualTo 1
        resultMap[1]?.get(0)?.name shouldBeEqualTo "chainOne"
        resultMap[2]?.size shouldBeEqualTo 2
        resultMap[2]?.get(1)?.name shouldBeEqualTo "chainTwoTwo"
        resultMap[3]?.size shouldBeEqualTo 3
        resultMap[3]?.get(2)?.name shouldBeEqualTo "chainOneThreeThreeThree"

        val resultEmptyMap = tokenManager.sortTokensByChainId(listOf())
        resultEmptyMap.size shouldBeEqualTo 0
    }

    @Test
    fun `Check merging ERC20Token maps`() {
        val tokensSetOne = tokenManager.sortTokensByChainId(
            listOf(
                ERCToken(1, "tokenOneOne1", address = "theSame", accountAddress = "accountAddress1", type = TokenType.ERC20),
                ERCToken(1, "tokenOneOne5", address = "0x0NE0N3", accountAddress = "accountAddress0", type = TokenType.ERC20),

                ERCToken(2, "tokenTwoOne", address = "0xTW00N3", accountAddress = "accountAddress1", type = TokenType.ERC20),
                ERCToken(2, "tokenTwoTwo", address = "0xTW0TW0", accountAddress = "accountAddress2", type = TokenType.ERC20),

                ERCToken(3, "tokenThreeOne", address = "0xTHR330N3", accountAddress = "accountAddress4", type = TokenType.ERC20),
                ERCToken(3, "tokenThreeTwo", address = "0xTHR33TW0", accountAddress = "accountAddress2", type = TokenType.ERC20),
                ERCToken(3, "tokenThreeThree", address = "0xTHR33THR33", accountAddress = "accountAddress1", type = TokenType.ERC721)
            )
        )

        val tokenSetTwo = tokenManager.sortTokensByChainId(
            listOf(
                ERCToken(1, "tokenOneOne", address = "0x0NE0N3", type = TokenType.ERC20)
            )
        )

        val tokenSetThree = tokenManager.sortTokensByChainId(
            listOf(
                ERCToken(5, "tokenFive", address = "0xFIV3", type = TokenType.ERC20)
            )
        )

        val localTokens = tokenManager.sortTokensByChainId(
            listOf(
                ERCToken(
                    1,
                    "tokenOneOne1",
                    address = "theSame",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress1",
                    type = TokenType.ERC20
                ),
                ERCToken(
                    1,
                    "tokenOneOne1",
                    address = "newAddress",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress2",
                    type = TokenType.ERC20
                ),
                ERCToken(
                    1,
                    "tokenOneOne2",
                    address = "0x0NE0N31",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress3",
                    type = TokenType.ERC20
                ),

                ERCToken(2, "tokenTwo", address = "0xS2Two01", accountAddress = "accountAddress1", type = TokenType.ERC20),
                ERCToken(2, "tokenTwo2", address = "0xS2Two02", accountAddress = "accountAddress2", type = TokenType.ERC20),

                ERCToken(
                    3,
                    "tokenThreeThree",
                    address = "0xTHR33THR33",
                    logoURI = "bb1",
                    accountAddress = "accountAddress1",
                    type = TokenType.ERC20
                ),
                ERCToken(
                    3,
                    "tokenThreeThree2",
                    address = "address3",
                    logoURI = "bb",
                    accountAddress = "accountAddress1",
                    type = TokenType.ERC721
                )
            )
        )
        whenever(walletManager.getWalletConfig()).thenReturn(WalletConfig(1, erc20Tokens = localTokens))

        val mergedTokenMap01 = tokenManager.mergeWithLocalTokensList(tokensSetOne)
        mergedTokenMap01.shouldSafeNewTokens shouldBeEqualTo true

        mergedTokenMap01.tokensPerChainIdMap[1]?.size shouldBeEqualTo 4
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(0)?.name shouldBeEqualTo "tokenOneOne1"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(0)?.accountAddress shouldBeEqualTo "accountAddress1"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(0)?.address shouldBeEqualTo "theSame"

        mergedTokenMap01.tokensPerChainIdMap[1]?.get(1)?.name shouldBeEqualTo "tokenOneOne1"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(1)?.accountAddress shouldBeEqualTo "accountAddress2"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(1)?.address shouldBeEqualTo "newAddress"

        mergedTokenMap01.tokensPerChainIdMap[1]?.get(2)?.name shouldBeEqualTo "tokenOneOne2"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(2)?.accountAddress shouldBeEqualTo "accountAddress3"

        mergedTokenMap01.tokensPerChainIdMap[1]?.get(3)?.name shouldBeEqualTo "tokenOneOne5"
        mergedTokenMap01.tokensPerChainIdMap[1]?.get(3)?.accountAddress shouldBeEqualTo "accountAddress0"

        mergedTokenMap01.tokensPerChainIdMap[2]?.size shouldBeEqualTo 4
        mergedTokenMap01.tokensPerChainIdMap[2]?.get(0)?.name shouldBeEqualTo "tokenTwo"

        mergedTokenMap01.tokensPerChainIdMap[2]?.get(1)?.name shouldBeEqualTo "tokenTwo2"

        mergedTokenMap01.tokensPerChainIdMap[2]?.get(2)?.name shouldBeEqualTo "tokenTwoOne"
        mergedTokenMap01.tokensPerChainIdMap[2]?.get(2)?.logoURI shouldBeEqualTo null

        mergedTokenMap01.tokensPerChainIdMap[2]?.get(3)?.name shouldBeEqualTo "tokenTwoTwo"

        mergedTokenMap01.tokensPerChainIdMap[3]?.size shouldBeEqualTo 5
        mergedTokenMap01.tokensPerChainIdMap[3]?.get(0)?.name shouldBeEqualTo "tokenThreeThree"
        mergedTokenMap01.tokensPerChainIdMap[3]?.get(0)?.logoURI shouldBeEqualTo "bb1"
        mergedTokenMap01.tokensPerChainIdMap[3]?.get(1)?.name shouldBeEqualTo "tokenThreeThree2"
        mergedTokenMap01.tokensPerChainIdMap[3]?.get(1)?.logoURI shouldBeEqualTo "bb"

        val mergedTokenMap02 = tokenManager.mergeWithLocalTokensList(tokenSetTwo)
        mergedTokenMap02.shouldSafeNewTokens shouldBeEqualTo true
        mergedTokenMap02.tokensPerChainIdMap[1]?.size shouldBeEqualTo 4
        mergedTokenMap02.tokensPerChainIdMap[1]?.get(0)?.logoURI shouldBeEqualTo "logoOneOne"

        val mergedTokenMap03 = tokenManager.mergeWithLocalTokensList(tokenSetThree)
        mergedTokenMap03.shouldSafeNewTokens shouldBeEqualTo true
        mergedTokenMap03.tokensPerChainIdMap.size shouldBeEqualTo 4
        mergedTokenMap03.tokensPerChainIdMap[5]?.size shouldBeEqualTo 1
        mergedTokenMap03.tokensPerChainIdMap[5]?.get(0)?.name shouldBeEqualTo "tokenFive"
        mergedTokenMap03.tokensPerChainIdMap[1]?.size shouldBeEqualTo 3
        mergedTokenMap03.tokensPerChainIdMap[2]?.size shouldBeEqualTo 2
        mergedTokenMap03.tokensPerChainIdMap[3]?.size shouldBeEqualTo 2
    }

    private val data = listOf(
        TokenDetails(1, "0x4ddre55", "LogoUri"),
        TokenDetails(1, "0x0NE0N3", "logoOneOne"),
        TokenDetails(2, "0x4ddre55", "LogoUri2"),
        TokenDetails(2, "0xS2Two01", "logoTwo"),
        TokenDetails(2, "0xS2Two01", "logoTwo", _tags = listOf(TokenTag.SUPER_TOKEN.tag)),
        TokenDetails(23, "---", "---")

    )

    @Test
    fun `Check getting Token Icon URL method`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.just(data), Single.just(listOf()))
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

        verify(cryptoApi, times(2)).getTokenDetails(any())
    }

    @Test
    fun `getting token rate test`() {
        whenever(rateStorage.getRate(any())).thenReturn(3.3)
        tokenManager.getSingleTokenRate("somesome") shouldBeEqualTo 3.3
    }

    @Test
    fun `Check mapping last commit data to last commit timestamp`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(cryptoApi.getLastCommitFromTokenList(any())).thenReturn(Single.just(commitData))
        whenever(cryptoApi.getNftCollectionDetails()).thenReturn(Single.just(emptyList()))
        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.just(data))
        whenever(localStorage.loadTokenIconsUpdateTimestamp()).thenReturn(333L, 1611950162000, 1611950162333)
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(tokenDao.getTaggedTokens()).thenReturn(
            Single.just(
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "CookieTokenATS",
                        "Cookie",
                        "0xS0m3T0k3N",
                        "13",
                        type = TokenType.ERC20
                    )
                )
            )
        )
        doNothing().whenever(tokenDao).updateTaggedTokens(any())
        doNothing().whenever(localStorage).saveFreeATSTimestamp(any())
        tokenManager.checkMissingTokensDetails().test().assertComplete()
        tokenManager.checkMissingTokensDetails().test().assertNotComplete()
        tokenManager.checkMissingTokensDetails().test().assertNotComplete()
    }

    @Test
    fun `Creating correct token URLs`() {
        val accountOne = Account(1, chainId = ATS_TAU, address = "0xADDRESSxONE")
        val accountTwo = Account(1, chainId = ETH_ROP, address = "0xADDRESSxTWO")
        val accountThree = Account(1, chainId = POA_SKL, address = "0xADDRESSxTHREE")
        val accountFour = Account(1, chainId = LUKSO_14, address = "0xADDRESSxFOUR")
        val accountFive = Account(1, chainId = ATS_SIGMA, address = "0xADDRESSxFIVE")
        val accountSix = Account(1, chainId = GNO, address = "0xADDRESSxSIX")
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
        val atsTauAccount = Account(1, chainId = ATS_TAU, address = "address4455")
        val tauTokenResponse01 = Flowable.just(TokenWithBalance(ATS_TAU, "0xC00k1eN", 10000.toBigDecimal()) as Token)
        val tauTokenResponse02 = Flowable.just(TokenWithBalance(ATS_TAU, "0xS0m3T0k3N", 100000000.toBigDecimal()) as Token)
        val tauTokenResponse03 = Flowable.just(TokenWithBalance(ATS_TAU, "0xC00k1e", 100000000.toBigDecimal()) as Token)
        val tauTokenResponse04 = Flowable.just(TokenWithBalance(ATS_TAU, "0x0th3r", 100000000.toBigDecimal()) as Token)
        val tauTokenResponse05 = Flowable.just(TokenWithBalance(ATS_TAU, "0xC00k14", BigDecimal.ONE, "1") as Token)
        val tauTokenResponse06 = Flowable.just(TokenWithBalance(ATS_TAU, "0xC00k14", BigDecimal.TEN, "1") as Token)

        val atsSigmaAccount = Account(246529, chainId = ATS_SIGMA, address = "0xADDRESSxTWO")
        val sigmaTokenResponse01 = Flowable.just(TokenWithBalance(ATS_SIGMA, "0xC00k1e", 10000.toBigDecimal()) as Token)
        val sigmaTokenResponse02 = Flowable.just(TokenWithBalance(ATS_SIGMA, "0x0th3r22", 10000.toBigDecimal()) as Token)
        val sigmaTokenResponse03 = Flowable.just(TokenWithBalance(ATS_SIGMA, "0x0th3r", 10000.toBigDecimal()) as Token)

        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(erc20TokenRepository.getTokenBalance(any(), any(), any(), any()))
            .thenReturn(
                tauTokenResponse01, tauTokenResponse02, tauTokenResponse03, tauTokenResponse04,
                sigmaTokenResponse01, sigmaTokenResponse02, sigmaTokenResponse03

            )
        whenever(erc721TokenRepository.getTokenBalance(any(), any(), any(), any(), any()))
            .thenReturn(tauTokenResponse05)
        whenever(erc1155TokenRepository.getTokenBalance(any(), any(), any(), any(), any()))
            .thenReturn(tauTokenResponse06)

        whenever(rateStorage.getRate(any())).thenReturn(2.0)

        tokenManager.getTokenBalance(atsTauAccount)
            .test()
            .assertNoErrors()
            .assertValueCount(4)
            .assertValueAt(
                0,
                AssetBalance(
                    chainId = 246785,
                    privateKey = "",
                    accountToken = AccountToken(
                        token = ERCToken(
                            chainId = ATS_TAU,
                            name = "CookieTokenATS",
                            symbol = "Cookie",
                            address = "0xC00k1eN",
                            decimals = "13",
                            accountAddress = "",
                            logoURI = null,
                            tag = "",
                            type = TokenType.ERC20,
                            isError = false
                        ), currentRawBalance = BigDecimal(10000), tokenPrice = 2.0
                    )
                )
            )
    }

    @Test
    fun `check getting tokens rate request`() {
        val error = Throwable("ERROR-333")
        val rates = mapOf(Pair("40x0th3r", 1.0), Pair("40xc00k1e", 0.2), Pair("hash03", 3.3))
        val tokensRateResponse = mapOf("id" to (mapOf("tokenName" to "3.3")))
        val tokens = mapOf(Pair(1, listOf(firstToken, secondToken)), Pair(3, listOf(firstTokenII, secondTokenII)))

        doNothing().whenever(rateStorage).saveRate(any(), any())
        whenever(localStorage.getTokenVisibilitySettings()).thenReturn(spy(TokenVisibilitySettings()))
        whenever(rateStorage.getRates()).thenReturn(rates)
        tokens.forEach { (chainId, tokens) ->
            tokens.forEach { token ->
                whenever(rateStorage.shouldUpdateRate(generateTokenHash(token.chainId, token.address)))
                    .thenReturn(true, false, false, false)
                whenever(tokenManager.getTokenVisibility(token.accountAddress, token.address)).thenReturn(true)
            }
        }
        whenever(cryptoApi.getTokensRate(any(), any(), any())).thenReturn(
            Single.just(tokensRateResponse),
            Single.just(tokensRateResponse),
            Single.just(tokensRateResponse),
            Single.just(tokensRateResponse),
            Single.error(error)
        )
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")

        tokenManager.getTokensRates(tokens)
            .test()
            .assertComplete()
        tokenManager.getTokensRates(tokens)
            .test()
            .assertComplete()

        verify(rateStorage, times(3)).saveRate(any(), any())
        verify(rateStorage, times(4)).shouldUpdateRate(any())
        verify(cryptoApi, times(1)).getTokensRate(any(), any(), any())
    }

    @Test
    fun `Check updating Tokens Rates`() {
        val accountTokens = mutableListOf(
            AccountToken(ERCToken(3, "one", address = "0x01", type = TokenType.ERC20), BigDecimal.TEN),
            AccountToken(ERCToken(3, "tow", address = "0x02", type = TokenType.ERC20), BigDecimal.TEN)
        )
        val account = Account(1, name = "account01", accountTokens = accountTokens)
        whenever(rateStorage.getRate(any())).thenReturn(0.1, 0.3)

        tokenManager.updateTokensRate(account)

        account.accountTokens[0].tokenPrice shouldBeEqualTo 0.1
        account.accountTokens[1].tokenPrice shouldBeEqualTo 0.3
    }

    @Test
    fun `get super token balance success test`() {
        val tokens = listOf(
            ERCToken(
                ATS_TAU,
                "name1",
                address = "address1",
                tag = "SuperToken",
                accountAddress = "address4455",
                type = TokenType.SUPER_TOKEN
            ),
            ERCToken(1, "name2", address = "address2", tag = "SuperToken", accountAddress = "test", type = TokenType.SUPER_TOKEN)
        )
        val account = Account(1, chainId = ATS_TAU, address = "address4455")
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletManager.getWalletConfig())
            .thenReturn(WalletConfig(
                1,
                erc20Tokens = tokenManager.sortTokensByChainId(tokens),
                accounts = listOf(account)
            ))
        whenever(erc20TokenRepository.getTokenBalance(any(), any(), any(), any())).thenReturn(
            Flowable.just(TokenWithBalance(1, "address1", BigDecimal.TEN))
        )
        whenever(rateStorage.getRate(any())).thenReturn(3.3)
        whenever(superTokenRepository.getNetFlow(any(), any(), any(), any(), any())).thenReturn(Flowable.just(BigInteger.TEN))

        tokenManager.getSuperTokenBalance(account)
            .test()
            .await()
            .assertNoErrors()
            .assertValueCount(1)
            .assertValue { asset ->
                asset is AssetBalance &&
                        asset.chainId == ATS_TAU &&
                        asset.accountToken.tokenPrice == 3.3 &&
                        asset.accountToken.currentRawBalance == BigDecimal.TEN
            }
    }

    @Test
    fun `get super token balance failure test`() {
        val error = Throwable("Get super token flowable")
        val tokens = listOf(
            ERCToken(
                ATS_TAU,
                "name1",
                address = "address1",
                tag = "SuperToken",
                accountAddress = "address4455",
                type = TokenType.SUPER_TOKEN
            ),
            ERCToken(1, "name2", address = "address2", tag = "SuperToken", accountAddress = "test", type = TokenType.SUPER_TOKEN)
        )
        val account = Account(1, chainId = ATS_TAU, address = "address4455")
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletManager.getWalletConfig())
            .thenReturn(WalletConfig(
                1,
                erc20Tokens = tokenManager.sortTokensByChainId(tokens),
                accounts = listOf(account)
            ))
        whenever(erc20TokenRepository.getTokenBalance(any(), any(), any(), any())).thenReturn(
            Flowable.just(TokenWithError(1, "address1", error))
        )
        whenever(rateStorage.getRate(any())).thenReturn(3.3)
        whenever(superTokenRepository.getNetFlow(any(), any(), any(), any(), any())).thenReturn(Flowable.just(BigInteger.TEN))

        tokenManager.getSuperTokenBalance(account)
            .test()
            .await()
            .assertValue { asset ->
                asset is AssetError &&
                        asset.error.message == "Get super token flowable"
            }
    }


    @Test
    fun `test should update nft details`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokensMap = mapOf(
            Pair(
                ATS_TAU,
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "nftToken",
                        "NFT",
                        "tokenAddress",
                        accountAddress = "accountAddress",
                        tokenId = "2",
                        type = TokenType.ERC721
                    )
                )
            )
        )
        val accounts = listOf(Account(1, chainId = ATS_TAU, address = "accountAddress", privateKey = "privateKey"))
        whenever(erc721TokenRepository.getERC721DetailsUri(any(), any(), any(), any())).thenReturn(Single.just("detailsUrl"))
        whenever(cryptoApi.getERC721TokenDetails(any())).thenReturn(
            Single.just(
                NftDetails(
                    "nftToken",
                    "contentUri",
                    "animationUrl",
                    "description",
                    "background"
                )
            )
        )

        tokenManager.updateMissingNFTTokensDetails(tokensMap, accounts)
            .test()
            .await()
            .assertValue { result ->
                val updatedToken = result.tokensPerChainIdMap[ATS_TAU]?.first()
                result.shouldSafeNewTokens && updatedToken?.nftContent?.imageUri == "contentUri" && updatedToken.nftContent.description == "description"
            }
    }

    @Test
    fun `test should update to data from remote config`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokensMap = mapOf(
            Pair(
                ATS_TAU,
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "nftToken",
                        "NFT",
                        "tokenAddress",
                        collectionName = "nftToken",
                        accountAddress = "accountAddress",
                        tokenId = "1",
                        type = TokenType.ERC721
                    ),
                    ERCToken(
                        ATS_TAU,
                        "nftToken2",
                        "NFT2",
                        "tokenAddress2",
                        collectionName = "nftToken2",
                        accountAddress = "accountAddress2",
                        tokenId = "2",
                        type = TokenType.ERC721
                    )
                )
            )
        )
        whenever(cryptoApi.getNftCollectionDetails()).thenReturn(
            Single.just(
                listOf(
                    NftCollectionDetails(ATS_TAU, "tokenAddress", "logoUri1", "newNftToken", "nNFT", true),
                    NftCollectionDetails(ATS_TAU, "tokenAddress2", "logoUri2", "nftToken", "NFT", false)
                )
            )
        )

        tokenManager.mergeNFTDetailsWithRemoteConfig(true, tokensMap)
            .test()
            .await()
            .assertValue { result ->
                 result.tokensPerChainIdMap[ATS_TAU]?.first()?.let { updatedToken ->
                     updatedToken.logoURI == "logoUri1"
                             && updatedToken.symbol == "nNFT"
                             && updatedToken.collectionName == "newNftToken"
                } ?: false
            }
            .assertValue{result ->
                val asd = result.tokensPerChainIdMap[ATS_TAU]?.last()
                result.tokensPerChainIdMap[ATS_TAU]?.last()?.let{updatedToken ->
                    updatedToken.logoURI == "logoUri2"
                            && updatedToken.symbol == "NFT2"
                            && updatedToken.collectionName == "nftToken2"
                } ?: false
            }
    }

    @Test
    fun `test should update collection logo url`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokensMap = mapOf(
            Pair(
                ATS_TAU,
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "nftToken",
                        "NFT",
                        "tokenAddress",
                        accountAddress = "accountAddress",
                        tokenId = "2",
                        type = TokenType.ERC721
                    )
                )
            )
        )
        whenever(cryptoApi.getNftCollectionDetails()).thenReturn(
            Single.just(
                listOf(NftCollectionDetails(ATS_TAU, "tokenAddress", "logoUri", "nftToken", "NFT"))
            )
        )

        tokenManager.mergeNFTDetailsWithRemoteConfig(true, tokensMap)
            .test()
            .await()
            .assertValue { result ->
                val updatedToken = result.tokensPerChainIdMap[ATS_TAU]?.first()
                result.shouldSafeNewTokens && updatedToken?.logoURI == "logoUri"
            }
    }

    @Test
    fun `test shouldn't update collection logo url`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val tokensMap = mapOf(
            Pair(
                ATS_TAU,
                listOf(
                    ERCToken(
                        ATS_TAU,
                        "nftToken",
                        "NFT",
                        "tokenAddress",
                        accountAddress = "accountAddress",
                        tokenId = "2",
                        type = TokenType.ERC721
                    )
                )
            )
        )
        whenever(cryptoApi.getNftCollectionDetails()).thenReturn(
            Single.just(
                listOf(NftCollectionDetails(ATS_TAU, "tokenAddress1", "logoUri", "nftToken", "NFT"))
            )
        )

        tokenManager.mergeNFTDetailsWithRemoteConfig(true, tokensMap)
            .test()
            .await()
            .assertValue { result ->
                val updatedToken = result.tokensPerChainIdMap[ATS_TAU]?.first()
                result.shouldSafeNewTokens && updatedToken?.logoURI == null
            }
    }

    @Test
    fun `test download tokens owned`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(1, chainId = GNO, address = "accountAddress", privateKey = "privateKey")
        whenever(cryptoApi.getTokensOwned(any())).thenReturn(
            Single.just(
                TokensOwnedPayload(
                    "",
                    listOf(
                        TokensOwnedPayload.TokenOwned(
                            "1",
                            "address01",
                            "18",
                            emptyList(),
                            "88",
                            "Name",
                            "Symbol",
                            "uri",
                            listOf("ERC-1155")
                        ),
                        TokensOwnedPayload.TokenOwned(
                            "10",
                            "address02",
                            "18",
                            emptyList(),
                            "88",
                            "n4m8",
                            "Symbol",
                            "uri",
                            listOf("ERC-721")
                        ),
                        TokensOwnedPayload.TokenOwned(
                            "1000",
                            "address03",
                            "18",
                            emptyList(),
                            "",
                            "Nam3",
                            "Symb0l",
                            "uri",
                            listOf("ERC-20"),
                            TokensOwnedPayload.TokenOwned.TokenJson.Empty
                        )
                    ),
                    ""
                )
            )
        )

        tokenManager.downloadTokensList(account)
            .test()
            .await()
            .assertValue {
                    result -> result.size == 3
                        && result.first().chainId == GNO
                        && result.first().name == String.Empty
                        && result.first().collectionName == "Name"
                        && result.first().symbol == "Symbol"
                        && result.first().address == "address01"
                        && result.first().decimals == "18"
                        && result.first().accountAddress == "accountAddress"
                        && result.first().tokenId == "88"
                        && result.first().type == TokenType.ERC1155
                        && result.last().chainId == GNO
                        && result.last().name == "Nam3"
                        && result.last().collectionName == null
                        && result.last().symbol == "Symb0l"
                        && result.last().address == "address03"
                        && result.last().decimals == "18"
                        && result.last().accountAddress == "accountAddress"
                        && result.last().type.isERC20()
            }
    }

    @Test
    fun `test download tokens from transactions`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(1, chainId = ETH_ROP, address = "accountAddress", privateKey = "privateKey")
        whenever(cryptoApi.getTokenTx(any())).thenReturn(
            Single.just(
                TokenTxResponse(
                    tokens =
                    listOf(
                        TokenTx(
                            tokenName = "token1",
                            tokenSymbol = "TK1",
                            address = "address1",
                            tokenDecimal = "12",
                            tokenId = "1"
                        )
                    )
                )
            )
        )

        tokenManager.downloadTokensList(account)
            .test()
            .await()
            .assertValue { result ->
                result.size == 2 &&
                        result.first().chainId == ETH_ROP &&
                        result.first().name == "token1" &&
                        result.first().collectionName == null &&
                        result.first().symbol == "TK1" &&
                        result.first().address == "address1" &&
                        result.first().decimals == "12" &&
                        result.first().accountAddress == "accountAddress" &&
                        result.first().tokenId == "1" &&
                        result.first().type == TokenType.ERC20 &&
                        result.last().chainId == ETH_ROP &&
                        result.last().name == "" &&
                        result.last().collectionName == "token1" &&
                        result.last().symbol == "TK1" &&
                        result.last().address == "address1" &&
                        result.last().decimals == "12" &&
                        result.last().accountAddress == "accountAddress" &&
                        result.last().tokenId == "1" &&
                        result.last().type.isERC721()
            }
    }

    @Test
    fun `test download tokens`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(1, chainId = POA_CORE, address = "accountAddress", privateKey = "privateKey")
        whenever(cryptoApi.getTokenTx(any())).thenReturn(
            Single.just(
                TokenTxResponse(
                    tokens =
                    listOf(
                        TokenTx(
                            tokenName = "token2",
                            tokenSymbol = "TK2",
                            address = "address2",
                            tokenDecimal = "",
                            tokenId = "1"
                        ),
                        TokenTx(
                            tokenName = "token3",
                            tokenSymbol = "TK3",
                            address = "address3",
                            tokenDecimal = "",
                            tokenId = "1"
                        )
                    )
                )
            )
        )
        whenever(erc721TokenRepository.isTokenOwner(any(), any(),any(), any(),any())).thenReturn(Single.just(true))
        whenever(cryptoApi.getConnectedTokens(any())).thenReturn(
            Single.just(
                TokenBalanceResponse(
                    tokens =
                    listOf(
                        TokenData(
                            name = "token1",
                            symbol = "TK1",
                            address = "address1",
                            decimals = "12",
                            type = Tokens.ERC_20.type
                        ),
                        TokenData(
                            name = "token2",
                            symbol = "TK2",
                            address = "address2",
                            decimals = "",
                            type = Tokens.ERC_721.type,
                            balance = "1"
                        ),
                        TokenData(
                            name = "token3",
                            symbol = "TK3",
                            address = "address3",
                            decimals = "",
                            type = Tokens.ERC_1155.type,
                            balance = "1"
                        )
                    )
                )
            )
        )

        tokenManager.downloadTokensList(account)
            .test()
            .await()
            .assertValue { result ->
                result.size == 3 &&
                        result.first().chainId == POA_CORE &&
                        result.first().name == "token1" &&
                        result.first().collectionName == null &&
                        result.first().symbol == "TK1" &&
                        result.first().address == "address1" &&
                        result.first().decimals == "12" &&
                        result.first().accountAddress == "accountAddress" &&
                        result.first().tokenId == null &&
                        result.first().type == TokenType.ERC20 &&
                        result[1].chainId == POA_CORE &&
                        result[1].name == "" &&
                        result[1].collectionName == "token2" &&
                        result[1].symbol == "TK2" &&
                        result[1].address == "address2" &&
                        result[1].decimals == "" &&
                        result[1].accountAddress == "accountAddress" &&
                        result[1].tokenId == "1" &&
                        result[1].type.isERC721() &&
                        result[2].chainId == POA_CORE &&
                        result[2].name == "" &&
                        result[2].collectionName == "token3" &&
                        result[2].symbol == "TK3" &&
                        result[2].address == "address3" &&
                        result[2].decimals == "" &&
                        result[2].accountAddress == "accountAddress" &&
                        result[2].tokenId == "1" &&
                        result[2].type.isERC1155()
            }
    }

    @Test
    fun `test download tokens for mumbai`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(1, chainId = MUMBAI, address = "accountAddress", privateKey = "privateKey")
        tokenManager.downloadTokensList(account)
            .test()
            .await()
            .assertValue { result ->
                result.isEmpty()
            }
    }

    @Test
    fun `test get nft for account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val collectionAddress = "collectionAddress"
        val token1 = ERCToken(
            GNO,
            "token1",
            address = collectionAddress,
            accountAddress = "accountAddress",
            type = TokenType.ERC721,
            tokenId = "1"
        )
        val token2 = ERCToken(
            GNO,
            "token2",
            address = collectionAddress,
            accountAddress = "accountAddress",
            type = TokenType.ERC721,
            tokenId = "2"
        )
        val token3 = ERCToken(
            GNO,
            "token3",
            address = collectionAddress,
            accountAddress = "accountAddress",
            type = TokenType.ERC721,
            tokenId = "3"
        )
        val token4 = ERCToken(
            GNO,
            "erc1155",
            address = collectionAddress,
            accountAddress = "accountAddress",
            type = TokenType.ERC1155,
            tokenId = "4"
        )
        val token5 = ERCToken(
            GNO,
            "erc1155",
            address = collectionAddress,
            accountAddress = "accountAddress",
            type = TokenType.ERC1155,
            tokenId = "5"
        )
        val localTokens = tokenManager.sortTokensByChainId(listOf(token1, token2, token3, token4, token5))
        whenever(walletManager.getWalletConfig()).thenReturn(WalletConfig(1, erc20Tokens = localTokens))
        val account = Account(1, chainId = GNO, address = "accountAddress", privateKey = "privateKey")
        tokenManager.getNftsPerAccount(GNO, account.address, collectionAddress)[0] shouldBeEqualTo token1
        tokenManager.getNftsPerAccount(GNO, account.address, collectionAddress)[1] shouldBeEqualTo token2
        tokenManager.getNftsPerAccount(GNO, account.address, collectionAddress)[2] shouldBeEqualTo token3
        tokenManager.getNftsPerAccount(GNO, account.address, collectionAddress)[3] shouldBeEqualTo token4
        tokenManager.getNftsPerAccount(GNO, account.address, collectionAddress)[4] shouldBeEqualTo token5
    }
}