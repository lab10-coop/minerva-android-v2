package minerva.android.walletmanager.manager.accounts.tokens

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.Commit
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.Committer
import minerva.android.apiProvider.model.TokenDetails
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
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
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenTag
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.utils.MockDataProvider
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
    private val rateStorage: RateStorage = mock()
    private val tokenDao: TokenDao = mock()
    private lateinit var database: MinervaDatabase
    private lateinit var tokenManager: TokenManagerImpl

    @Before
    fun initializeMocks() {
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig, MockDataProvider.walletConfig)
        whenever(walletManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        database =  mock { whenever(mock.tokenDao()).thenReturn(tokenDao) }
        tokenManager =
            TokenManagerImpl(walletManager, cryptoApi, localStorage, blockchainRepository, rateStorage, database)
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
        val firstToken = ERC20Token(1, "CookieToken", "COOKiE", "0xC00k1e", "C00")
        tokenManager.saveToken(ATS_TAU, firstToken).test().assertComplete()
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
    fun `Test tokens with online logos data without error`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.just(tokenRawData))
        tokenManager.updateTokenIcons(false, map).test().assertComplete().assertNoErrors()
            .assertValue {
                map[1]?.get(0)?.logoURI == null
            }
        tokenManager.updateTokenIcons(true, map).test().assertComplete().assertNoErrors()
            .assertValue {
                map[1]?.get(0)?.logoURI == "someIconAddress"
                map[1]?.get(1)?.logoURI == "someIconAddressII"
            }
    }

    @Test
    fun `Test tokens with online logos data with error`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.error(Throwable("No data here!")))
        tokenManager.updateTokenIcons(true, map).test().assertErrorMessage("No data here!")
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
        val newToken = ERC20Token(1, "SomeToken", "some", "0xt0k3n", "32", accountAddress = "address1")

        ATS_TAU.let { ATS ->
            val updatedTokens = tokenManager.updateTokens(ATS, newToken, MockDataProvider.walletConfig.erc20Tokens.toMutableMap())
            updatedTokens[ATS]?.size shouldBeEqualTo 5
            updatedTokens[ATS]?.get(0)?.name shouldBeEqualTo "CookieTokenATS"
            updatedTokens[ATS]?.get(1)?.name shouldBeEqualTo "OtherTokenATS1"
            updatedTokens[ATS]?.get(2)?.name shouldBeEqualTo "OtherTokenATS"
            updatedTokens[ATS]?.get(3)?.name shouldBeEqualTo "TokenTest1"
            updatedTokens[ATS]?.get(4)?.name shouldBeEqualTo "SomeToken"
            updatedTokens[ATS]?.get(4)?.accountAddress shouldBeEqualTo "address1"

            val secondNewToken = ERC20Token(1, "CookieCoin", "CC", "0xC00k1e", "32")
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
                    ERC20Token(ATS_TAU, "SomeToken01", "some01", "address1", "32", accountAddress = "accountADDress1", logoURI = "sd"),
                    ERC20Token(ATS_TAU, "SomeToken01", "some01", "address1", "32", accountAddress = "accountAddress1", logoURI = "sd"),

                    ERC20Token(ATS_TAU, "SomeToken02", "some01", "address2", "16", accountAddress = "accountAddress1"),
                    ERC20Token(ATS_TAU, "SomeToken03", "some02", "address3", "16", accountAddress = "accountAddress2")
                )
            ),
            Pair(
                ETH_RIN, listOf(
                    ERC20Token(ETH_RIN, "SomeToken03", "some03", "0xt0k3n03", "32"),
                    ERC20Token(ETH_RIN, "SomeToken04", "some04", "0xC00k1e", "16"),

                    ERC20Token(ETH_RIN, "SomeToken05", "some05", "ad1", "12"),
                    ERC20Token(ETH_RIN, "SomeToken05", "some05", "ad1", "12"),
                    ERC20Token(ETH_RIN, "SomeToken05", "some05", "ad1", "12")
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
            ERC20Token(1, "chainOne", address = "0x0N3"),
            ERC20Token(2, "chainTwo", address = "0x0N3"),
            ERC20Token(2, "chainTwoTwo", address = "0x0N3"),
            ERC20Token(3, "chainThree", address = "0x0N3"),
            ERC20Token(3, "chainThreeThree", address = "0x0N3"),
            ERC20Token(3, "chainOneThreeThreeThree", address = "0x0N3")
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
                ERC20Token(1, "tokenOneOne1", address = "theSame", accountAddress = "accountAddress1"),
                ERC20Token(1, "tokenOneOne5", address = "0x0NE0N3", accountAddress = "accountAddress0"),

                ERC20Token(2, "tokenTwoOne", address = "0xTW00N3", accountAddress = "accountAddress1"),
                ERC20Token(2, "tokenTwoTwo", address = "0xTW0TW0", accountAddress = "accountAddress2"),

                ERC20Token(3, "tokenThreeOne", address = "0xTHR330N3", accountAddress = "accountAddress4"),
                ERC20Token(3, "tokenThreeTwo", address = "0xTHR33TW0", accountAddress = "accountAddress2"),
                ERC20Token(3, "tokenThreeThree", address = "0xTHR33THR33", accountAddress = "accountAddress1")
            )
        )

        val tokenSetTwo = tokenManager.sortTokensByChainId(
            listOf(
                ERC20Token(1, "tokenOneOne", address = "0x0NE0N3")
            )
        )

        val tokenSetThree = tokenManager.sortTokensByChainId(
            listOf(
                ERC20Token(5, "tokenFive", address = "0xFIV3")
            )
        )

        val localTokens = tokenManager.sortTokensByChainId(
            listOf(
                ERC20Token(
                    1,
                    "tokenOneOne1",
                    address = "theSame",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress1"
                ),
                ERC20Token(
                    1,
                    "tokenOneOne1",
                    address = "newAddress",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress2"
                ),
                ERC20Token(
                    1,
                    "tokenOneOne2",
                    address = "0x0NE0N31",
                    logoURI = "logoOneOne",
                    accountAddress = "accountAddress3"
                ),

                ERC20Token(2, "tokenTwo", address = "0xS2Two01", accountAddress = "accountAddress1"),
                ERC20Token(2, "tokenTwo2", address = "0xS2Two02", accountAddress = "accountAddress2"),

                ERC20Token(
                    3,
                    "tokenThreeThree",
                    address = "0xTHR33THR33",
                    logoURI = "bb1",
                    accountAddress = "accountAddress1"
                ),
                ERC20Token(
                    3,
                    "tokenThreeThree2",
                    address = "address3",
                    logoURI = "bb",
                    accountAddress = "accountAddress1"
                )
            )
        )
        whenever(walletManager.getWalletConfig()).thenReturn(WalletConfig(1, erc20Tokens = localTokens))

        val mergedTokenMap01 = tokenManager.mergeWithLocalTokensList(tokensSetOne)
        mergedTokenMap01.first shouldBeEqualTo true

        mergedTokenMap01.second[1]?.size shouldBeEqualTo 4
        mergedTokenMap01.second[1]?.get(0)?.name shouldBeEqualTo "tokenOneOne1"
        mergedTokenMap01.second[1]?.get(0)?.accountAddress shouldBeEqualTo "accountAddress1"
        mergedTokenMap01.second[1]?.get(0)?.address shouldBeEqualTo "theSame"

        mergedTokenMap01.second[1]?.get(1)?.name shouldBeEqualTo "tokenOneOne1"
        mergedTokenMap01.second[1]?.get(1)?.accountAddress shouldBeEqualTo "accountAddress2"
        mergedTokenMap01.second[1]?.get(1)?.address shouldBeEqualTo "newAddress"

        mergedTokenMap01.second[1]?.get(2)?.name shouldBeEqualTo "tokenOneOne2"
        mergedTokenMap01.second[1]?.get(2)?.accountAddress shouldBeEqualTo "accountAddress3"

        mergedTokenMap01.second[1]?.get(3)?.name shouldBeEqualTo "tokenOneOne5"
        mergedTokenMap01.second[1]?.get(3)?.accountAddress shouldBeEqualTo "accountAddress0"

        mergedTokenMap01.second[2]?.size shouldBeEqualTo 4
        mergedTokenMap01.second[2]?.get(0)?.name shouldBeEqualTo "tokenTwo"

        mergedTokenMap01.second[2]?.get(1)?.name shouldBeEqualTo "tokenTwo2"

        mergedTokenMap01.second[2]?.get(2)?.name shouldBeEqualTo "tokenTwoOne"
        mergedTokenMap01.second[2]?.get(2)?.logoURI shouldBeEqualTo null

        mergedTokenMap01.second[2]?.get(3)?.name shouldBeEqualTo "tokenTwoTwo"

        mergedTokenMap01.second[3]?.size shouldBeEqualTo 4
        mergedTokenMap01.second[3]?.get(0)?.name shouldBeEqualTo "tokenThreeThree"
        mergedTokenMap01.second[3]?.get(0)?.logoURI shouldBeEqualTo "bb1"
        mergedTokenMap01.second[3]?.get(1)?.name shouldBeEqualTo "tokenThreeThree2"
        mergedTokenMap01.second[3]?.get(2)?.name shouldBeEqualTo "tokenThreeOne"
        mergedTokenMap01.second[3]?.get(2)?.logoURI shouldBeEqualTo null

        val mergedTokenMap02 = tokenManager.mergeWithLocalTokensList(tokenSetTwo)
        mergedTokenMap02.first shouldBeEqualTo true
        mergedTokenMap02.second[1]?.size shouldBeEqualTo 4
        mergedTokenMap02.second[1]?.get(0)?.logoURI shouldBeEqualTo "logoOneOne"

        val mergedTokenMap03 = tokenManager.mergeWithLocalTokensList(tokenSetThree)
        mergedTokenMap03.first shouldBeEqualTo true
        mergedTokenMap03.second.size shouldBeEqualTo 4
        mergedTokenMap03.second[5]?.size shouldBeEqualTo 1
        mergedTokenMap03.second[5]?.get(0)?.name shouldBeEqualTo "tokenFive"
        mergedTokenMap03.second[1]?.size shouldBeEqualTo 3
        mergedTokenMap03.second[2]?.size shouldBeEqualTo 2
        mergedTokenMap03.second[3]?.size shouldBeEqualTo 2
    }

    @Test
    fun `Check that tokens list has icon updates`() {
        val tokens = tokenManager.sortTokensByChainId(
            listOf(
                ERC20Token(1, "tokenOneOne", address = "0x0NE0N3", logoURI = "logoOne"),
                ERC20Token(2, "tokenTwo", address = "0xS2Two01", logoURI = "logoTwo"),
                ERC20Token(3, "tokenThreeThree", address = "0xTHR33THR33", logoURI = null)
            )
        )

        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.just(data))
        val updatedIcons = tokenManager.updateTokenIcons(true, tokens)
        val updatedIcons2 = tokenManager.updateTokenIcons(false, tokens)

        updatedIcons
            .test()
            .assertComplete()
            .assertValue {
                it.first &&
                        it.second.size == 3 &&
                        it.second[1]?.get(0)?.logoURI == "logoOneOne" &&
                        it.second[2]?.get(0)?.logoURI == "logoTwo" &&
                        it.second[3]?.get(0)?.logoURI == null
            }
        updatedIcons2
            .test()
            .assertComplete()
            .assertValue {
                !it.first &&
                        it.second.size == 3 &&
                        it.second[1]?.get(0)?.logoURI == "logoOneOne" &&
                        it.second[2]?.get(0)?.logoURI == "logoTwo" &&
                        it.second[3]?.get(0)?.logoURI == null
            }
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
        whenever(cryptoApi.getTokenDetails(any())).thenReturn(Single.just(data))
        whenever(localStorage.loadTokenIconsUpdateTimestamp()).thenReturn(333L, 1611950162000, 1611950162333)
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(tokenDao.getTaggedTokens()).thenReturn(
            Single.just(
                listOf(
                    ERC20Token(
                        ATS_TAU,
                        "CookieTokenATS",
                        "Cookie",
                        "0xS0m3T0k3N",
                        "13"
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
        val atsTauAccount = Account(1, chainId = ATS_TAU, address = "address4455")
        val tauTokenResponse01 = Observable.just(Pair("0xC00k1eN", 10000.toBigDecimal()))
        val tauTokenResponse02 = Observable.just(Pair("0xS0m3T0k3N", 100000000.toBigDecimal()))
        val tauTokenResponse03 = Observable.just(Pair("0xC00k1e", 100000000.toBigDecimal()))
        val tauTokenResponse04 = Observable.just(Pair("0x0th3r", 100000000.toBigDecimal()))

        val atsSigmaAccount = Account(2, chainId = ATS_SIGMA, address = "0xADDRESSxTWO")
        val sigmaTokenResponse01 = Observable.just(Pair("0xC00k1e", 10000.toBigDecimal()))
        val sigmaTokenResponse02 = Observable.just(Pair("0x0th3r22", 10000.toBigDecimal()))
        val sigmaTokenResponse03 = Observable.just(Pair("0x0th3r", 10000.toBigDecimal()))

        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(blockchainRepository.refreshTokenBalance(any(), any(), any(), any())).thenReturn(
            tauTokenResponse01, tauTokenResponse02, tauTokenResponse03, tauTokenResponse04,
            sigmaTokenResponse01, sigmaTokenResponse02, sigmaTokenResponse03
        )
        whenever(tokenDao.getTaggedTokens())
            .thenReturn(
                Single.just(
                    listOf(
                        ERC20Token(
                            ATS_TAU,
                            "testToken",
                            "symbol",
                            "0xS0m3T0k3N",
                            tag = "super1",
                            accountAddress = ""
                        ),
                        ERC20Token(
                            ATS_TAU,
                            "testToken",
                            "symbol",
                            "0xC00k1eN",
                            tag = "super2",
                            accountAddress = ""
                        ),
                        ERC20Token(
                            ATS_TAU,
                            "testToken",
                            "symbol",
                            "differentAddress",
                            tag = "super2",
                            accountAddress = ""
                        )
                    )
                )
            )

        whenever(rateStorage.getRate(any())).thenReturn(2.0)

        tokenManager.refreshTokensBalances(atsTauAccount)
            .test()
            .assertComplete()
            .assertValue {
                it.second.size == 5 &&
                        it.second[0].token.name == "CookieTokenATS" &&
                        it.second[0].balance.toPlainString() == "0.000000001" &&
                        it.second[0].token.tag == "super2" &&
                        it.second[1].token.name == "OtherTokenATS1" &&
                        it.second[1].token.tag == "super1" &&
                        it.second[1].token.accountAddress == ""
            }
        tokenManager.refreshTokensBalances(atsSigmaAccount)
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it.second.size == 3 &&
                        it.second[0].token.name == "SecondOtherATS" &&
                        it.second[0].balance.toPlainString() == "0.000000000000000001" &&
                        it.second[1].token.name == "OtherTokenATS" &&
                        it.second[1].balance.toPlainString() == "0.0000000000000000000000000001"
            }
    }

    @Test
    fun `check getting tokens rate request`() {
        val error = Throwable("ERROR-333")
        val rates = mapOf(Pair("40x0th3r", 1.0), Pair("40xc00k1e", 0.2), Pair("hash03", 3.3))
        val tokensRateResponse = mapOf("id" to (mapOf("tokenName" to "3.3")))
        val tokens = mapOf(Pair(1, listOf(firstToken, secondToken)), Pair(3, listOf(firstTokenII, secondTokenII)))

        doNothing().whenever(rateStorage).saveRate(any(), any())
        whenever(rateStorage.getRates()).thenReturn(rates)
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

        verify(rateStorage, times(2)).saveRate(any(), any())
        verify(cryptoApi, times(2)).getTokensRate(any(), any(), any())
    }

    @Test
    fun `Check updating Tokens Rates`() {
        val accountTokens = listOf(
            AccountToken(ERC20Token(3, "one", address = "0x01"), BigDecimal.TEN),
            AccountToken(ERC20Token(3, "tow", address = "0x02"), BigDecimal.TEN)
        )
        val account = Account(1, name = "account01", accountTokens = accountTokens)
        whenever(rateStorage.getRate(any())).thenReturn(0.1, 0.3)

        tokenManager.updateTokensRate(account)

        account.accountTokens[0].tokenPrice shouldBeEqualTo 0.1
        account.accountTokens[1].tokenPrice shouldBeEqualTo 0.3
    }

    private val commitData: List<CommitElement>
        get() = listOf(CommitElement(Commit(Committer("2021-01-29T19:56:02Z")))) //1611950162000 in mills


    private val firstToken = ERC20Token(ATS_TAU, "CookieToken", "COOKiE", "0xC00k1e", "1")
    private val secondToken = ERC20Token(ATS_TAU, "CookieTokenII", "COOKiE", "0xC00k1eII", "2")
    private val map = mapOf(Pair(1, listOf(firstToken, secondToken)))

    private val firstTokenII = ERC20Token(ETH_RIN, "CookieTokenRIN", "COOKiERIN", "0x0th3r", "1")
    private val secondTokenII = ERC20Token(ETH_RIN, "CookieTokenRINII", "COOKiERINII", "0xC00k1e", "2")

    private val tokenRawData = listOf(
        TokenDetails(ATS_TAU, "0xC00k1e", "someIconAddress"),
        TokenDetails(ATS_TAU, "0xC00k1eII", "someIconAddressII")
    )
}