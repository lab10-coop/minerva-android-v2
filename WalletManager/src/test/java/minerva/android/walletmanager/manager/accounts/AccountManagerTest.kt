package minerva.android.walletmanager.manager.accounts

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.exception.MissingAccountThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MockDataProvider
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class AccountManagerTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val timeProvider: CurrentTimeProvider = mock()
    private val blockchainRegularAccountRepository: BlockchainRegularAccountRepository = mock()
    private val manager = AccountManagerImpl(
        walletConfigManager,
        cryptographyRepository,
        blockchainRegularAccountRepository,
        localStorage,
        timeProvider
    )

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `Check that wallet manager creates new regular account`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address1")))

        val test = manager.createRegularAccount(Network()).test()
        test.assertNoErrors()
        manager.loadAccount(1).apply {
            id shouldBeEqualTo 2
            publicKey shouldBeEqualTo "publicKey2"
            privateKey shouldBeEqualTo "privateKey2"
            address shouldBeEqualTo "address1"
        }
    }

    @Test
    fun `Check that wallet manager don't save new regular account`() {
        val error = Throwable()
        val network = Network(chainId = 4, httpRpc = "some")
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn(String.Empty)
        whenever(
            cryptographyRepository.calculateDerivedKeysSingle(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val test = manager.createRegularAccount(network).test()
        test.assertError(error)
        manager.loadAccount(10).apply {
            id shouldBeEqualTo -1
            privateKey shouldBeEqualTo String.Empty
            publicKey shouldBeEqualTo String.Empty
            address shouldBeEqualTo String.Empty
        }
    }

    @Test
    fun `Check that wallet manager creates empty account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
            .thenReturn(DerivedKeys(0, "publicKey", "privateKey", "address1"))

        val test = manager.createEmptyAccounts(2).test()
        test.assertNoErrors()
        manager.loadAccount(1).apply {
            id shouldBeEqualTo 2
            publicKey shouldBeEqualTo "publicKey2"
            privateKey shouldBeEqualTo "privateKey2"
            address shouldBeEqualTo "address1"
        }
        manager.loadAccount(2).apply {
            id shouldBeEqualTo 3
            publicKey shouldBeEqualTo "publicKey3"
            privateKey shouldBeEqualTo "privateKey3"
            address shouldBeEqualTo "address1"
        }
    }

    @Test
    fun `Check that wallet manager connect network to empty account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(
                    1,
                    chainId = Int.InvalidValue,
                    publicKey = "publicKey",
                    privateKey = "privateKey",
                    address = "address1",
                    _isTestNetwork = true
                )
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")

        manager.connectAccountToNetwork(1, Network(chainId = 4, name = "Ethereum"))
        verify(walletConfigManager).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(
                        id = 1, publicKey = "publicKey", privateKey = "privateKey", address = "address1", name = "#2 Ethereum",
                        chainId = 4, isHide = false, _isTestNetwork = true
                    )
                )
            )
        )
    }

    @Test
    fun `Check that wallet manager create from empty account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(
                    1,
                    chainId = Int.InvalidValue,
                    publicKey = "publicKey",
                    privateKey = "privateKey",
                    address = "address1",
                    _isTestNetwork = true
                )
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")

        manager.createOrUnhideAccount(Network(chainId = 4, name = "Ethereum"))
        verify(walletConfigManager).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(
                        id = 1, publicKey = "publicKey", privateKey = "privateKey", address = "address1", name = "#2 Ethereum",
                        chainId = 4, isHide = false, _isTestNetwork = true
                    )
                )
            )
        )
    }

    @Test
    fun `Check that wallet manager unhide account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(
                    id = 1, publicKey = "publicKey", privateKey = "privateKey", address = "address1", name = "#2 Ethereum",
                    chainId = 4, isHide = true, _isTestNetwork = true
                )
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")

        manager.createOrUnhideAccount(Network(chainId = 4, name = "Ethereum"))
        verify(walletConfigManager).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(
                        id = 1, publicKey = "publicKey", privateKey = "privateKey", address = "address1", name = "#2 Ethereum",
                        chainId = 4, isHide = false, _isTestNetwork = true
                    )
                )
            )
        )
    }

    @Test
    fun `Check that wallet unhide account`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(
                    1, chainId = 4, name = "#2 Ethereum", publicKey = "publicKey", privateKey = "privateKey",
                    address = "address1", _isTestNetwork = true, isHide = true
                )
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")

        manager.connectAccountToNetwork(1, Network(chainId = 4, name = "Ethereum"))
        verify(walletConfigManager).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(
                        id = 1, publicKey = "publicKey", privateKey = "privateKey", address = "address1", name = "#2 Ethereum",
                        chainId = 4, isHide = false, _isTestNetwork = true
                    )
                )
            )
        )
    }

    @Test
    fun `Check that wallet manager removes correct empty value`() {
        val account = Account(2)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address1"))
        )
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        manager.removeAccount(account).test()
        val removedValue = manager.loadAccount(0)
        val notRemovedValue = manager.loadAccount(1)
        removedValue.id shouldBeEqualTo 1
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.id shouldBeEqualTo 2
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct not empty value`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(1)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey1", "privateKey1", "address1")))
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(300))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        doNothing().whenever(walletConfigManager).initWalletConfig()

        manager.removeAccount(account).test()
        val removedValue = manager.loadAccount(0)
        val notRemovedValue = manager.loadAccount(1)
        removedValue.id shouldBeEqualTo 1
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.id shouldBeEqualTo 2
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager don't removes correct safe account value`() {
        val account = Account(5)
        val account2 = Account(6)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(
            cryptographyRepository.calculateDerivedKeysSingle(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address1"))
        )
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        manager.removeAccount(account).test()
        manager.removeAccount(account2).test()
        manager.loadAccount(2).apply {
            id shouldBeEqualTo 3
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        manager.loadAccount(3).apply {
            id shouldBeEqualTo 4
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `Check that wallet manager removes correct safe account value`() {
        val account = Account(5)
        val account2 = Account(6)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address1")))
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        doNothing().whenever(walletConfigManager).initWalletConfig()

        manager.removeAccount(account).test()
        manager.removeAccount(account2).test()
        manager.loadAccount(2).apply {
            id shouldBeEqualTo 3
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        manager.loadAccount(3).apply {
            id shouldBeEqualTo 4
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `get safe account number test`() {
        val expected = Account(0, address = "123", privateKey = "key", owners = listOf("owner"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(
            accounts = listOf(
                expected
            )
        )
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 2
        manager.run {
            val result = getSafeAccountCount("owner")
            assertEquals(result, 2)
        }
    }

    @Test
    fun `get safe account number error test`() {
        val expected =
            Account(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(
            accounts = listOf(
                expected
            )
        )
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 1
        manager.run {
            val result = getSafeAccountCount("owner")
            assertEquals(result, 1)
        }
    }

    @Test
    fun `create safe account success`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("address")
        manager.createSafeAccount(Account(1, chainId = 4), "contract")
            .test()
            .assertComplete()
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("address")
        manager.createSafeAccount(Account(1, chainId = 4), "contract")
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account name test`() {
        val result =
            manager.getSafeAccountName(Account(1, address = "masterOwner", name = "test"))
        assertEquals("test", result)
    }

    @Test
    fun `is address valid success`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(true)
        val result = manager.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(false)
        val result = manager.isAddressValid("2342343")
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled returns true`() {
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(true)
        val result = manager.areMainNetworksEnabled
        assertEquals(true, result)
    }

    @Test
    fun `get token visibility test`() {
        whenever(localStorage.getTokenVisibilitySettings()).thenReturn(TokenVisibilitySettings())
        manager.getTokenVisibilitySettings
        verify(localStorage).getTokenVisibilitySettings()
    }

    @Test
    fun `save free ats timestamp test`() {
        doNothing().whenever(localStorage).saveFreeATSTimestamp(any())
        manager.saveFreeATSTimestamp()
        verify(localStorage).saveFreeATSTimestamp(0L)
    }

    @Test
    fun `get free ats timestamp visibility test`() {
        whenever(localStorage.loadLastFreeATSTimestamp()).thenReturn(0L)
        manager.getLastFreeATSTimestamp()
        verify(localStorage).loadLastFreeATSTimestamp()
    }

    @Test
    fun `save token visibility test`() {
        val visibility = TokenVisibilitySettings()
        whenever(localStorage.saveTokenVisibilitySettings(any())).thenReturn(visibility)
        manager.saveTokenVisibilitySettings(visibility)
        verify(localStorage).saveTokenVisibilitySettings(visibility)
    }

    @Test
    fun `get current timestamp test`() {
        whenever(timeProvider.currentTimeMills()).thenReturn(1L)
        val result = manager.currentTimeMills()
        verify(timeProvider).currentTimeMills()
        assertEquals(1L, result)
    }

    @Test
    fun `get all accounts test`() {
        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, accounts = listOf(Account(1))))
        val result = manager.getAllAccounts()
        assertEquals(result.get(0).id, 1)
    }

    @Test
    fun `getting all active accounts with given chainID`() {
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(1, chainId = 1, name = "account01", isDeleted = false),
                Account(2, chainId = 1, name = "account02", isDeleted = false),
                Account(3, chainId = 3, name = "account03", isDeleted = true),
                Account(4, chainId = 3, name = "account04", isDeleted = false),
                Account(5, chainId = 3, name = "account05", isDeleted = false)
            )
        )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        val accounts = manager.getAllActiveAccounts(3)
        accounts.size shouldBeEqualTo 2
        accounts[0].name shouldBeEqualTo "account04"
        accounts[1].name shouldBeEqualTo "account05"
    }

    @Test
    fun `to checksum address test`() {
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("checksum")
        val result = manager.toChecksumAddress("address")
        assertEquals(result, "checksum")
    }

    @Test
    fun `checking returning all accounts for selected networks type`() {
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(1, _isTestNetwork = true),
                Account(2, _isTestNetwork = false),
                Account(3, _isTestNetwork = false),
                Account(4, _isTestNetwork = true),
                Account(5, _isTestNetwork = true)
            )
        )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        val accounts = manager.getAllAccountsForSelectedNetworksType()
        accounts.size shouldBeEqualTo 3
        accounts[0].id shouldBeEqualTo 1
        accounts[1].id shouldBeEqualTo 4
        accounts[2].id shouldBeEqualTo 5
    }

    @Test
    fun `Checking clearing current fiats values for main coin and tokens`() {
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(
                    1, chainId = 1, name = "account01", isDeleted = false, fiatBalance = 13f.toBigDecimal(),
                    accountTokens = mutableListOf(
                        AccountToken(ERC20Token(1, "CookieToken", address = "0x0"), tokenPrice = 13.3),
                        AccountToken(ERC20Token(2, "AnotherToken", address = "0x1"), tokenPrice = 23.3)
                    )
                ),
                Account(
                    2, chainId = 1, name = "account02", isDeleted = false, fiatBalance = 3f.toBigDecimal(),
                    accountTokens = mutableListOf(
                        AccountToken(ERC20Token(3, "CookieToken", address = "0x0"), tokenPrice = 33.3),
                        AccountToken(ERC20Token(4, "CookieToken", address = "0x0"), tokenPrice = 43.3)
                    )
                )
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        walletConfigManager.getWalletConfig().accounts[0].apply {
            fiatBalance shouldBeEqualTo 13f.toBigDecimal()

        }

        walletConfigManager.getWalletConfig().accounts[0].fiatBalance shouldBeEqualTo 13f.toBigDecimal()
        walletConfigManager.getWalletConfig().accounts[0].fiatBalance shouldBeEqualTo 13f.toBigDecimal()
        manager.clearFiat()
        walletConfigManager.getWalletConfig().accounts[0].fiatBalance shouldBeEqualTo Double.InvalidValue.toBigDecimal()
    }

    @Test
    fun `test filtering cached tokens`() {
        manager.activeAccounts = listOf(
            Account(2, chainId = 1, address = "account1"),
            Account(3, chainId = 1, address = "account2")
        )
        whenever(localStorage.getTokenVisibilitySettings()).thenReturn(mock())
        with(localStorage.getTokenVisibilitySettings()) {
            whenever(getTokenVisibility("account1", "tokenAddress1")).thenReturn(true)
            whenever(getTokenVisibility("account1", "tokenAddress2")).thenReturn(true)
            whenever(getTokenVisibility("account1", "tokenAddress3")).thenReturn(false)
            whenever(getTokenVisibility("account1", "tokenAddress4")).thenReturn(true)

            whenever(getTokenVisibility("account2", "tokenAddress2")).thenReturn(false)
            whenever(getTokenVisibility("account2", "tokenAddress5")).thenReturn(true)
        }
        val result: Map<Int, List<ERC20Token>> = manager.filterCachedTokens(tokenMap)
        result[1]?.size shouldBeEqualTo 4
    }

    private val tokenMap = mapOf(
        1 to mutableListOf(
            ERC20Token(1, "token1", address = "tokenAddress1", accountAddress = "account1"),
            ERC20Token(1, "token2", address = "tokenAddress2", accountAddress = "account1"),
            ERC20Token(1, "token3", address = "tokenAddress3", accountAddress = "account1"),
            ERC20Token(1, "token4", address = "tokenAddress4", accountAddress = "account1"),

            ERC20Token(1, "token5", address = "tokenAddress2", accountAddress = "account2"),
            ERC20Token(1, "token6", address = "tokenAddress5", accountAddress = "account2")
        )
    )

    @Test
    fun `get active accounts on test networks test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(false)
        val config = WalletConfig(1, accounts = listOf(Account(1, chainId = ChainId.ATS_TAU)))
        val result = manager.getActiveAccounts(config)
        result.size shouldBeEqualTo 1
    }

    @Test
    fun `get active accounts on main networks test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(true)
        val config = WalletConfig(1, accounts = listOf(Account(1, chainId = 1)))
        val result = manager.getActiveAccounts(config)
        result.size shouldBeEqualTo 1
    }

    @Test
    fun `get all free addresses for network test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = false),
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = true),
                Account(2, chainId = ChainId.ATS_TAU, address = "address2", _isTestNetwork = true, isHide = false),
                Account(3, chainId = ChainId.ETH_RIN, address = "address3", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ETH_RIN, address = "address1", _isTestNetwork = true, isHide = false),
                Account(
                    4,
                    chainId = ChainId.ATS_SIGMA,
                    address = "address4",
                    _isTestNetwork = true,
                    isHide = true,
                    isDeleted = true
                ),
                Account(5, chainId = Int.InvalidValue, address = "address5", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ETH_RIN, address = "address6", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ATS_SIGMA, address = "address6", _isTestNetwork = true, isHide = true)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        val result = manager.getAllFreeAccountForNetwork(ChainId.ATS_TAU)
        result[0].first shouldBeEqualTo 1
        result[0].second shouldBeEqualTo "address1"
        result[1].first shouldBeEqualTo 3
        result[1].second shouldBeEqualTo "address3"
        result[2].first shouldBeEqualTo 5
        result[2].second shouldBeEqualTo "address5"
        result[3].first shouldBeEqualTo 6
        result[3].second shouldBeEqualTo "address6"
    }

    @Test
    fun `get number of accounts to use test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = false),
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = true),
                Account(2, chainId = ChainId.ATS_TAU, address = "address2", _isTestNetwork = true, isHide = false),
                Account(3, chainId = ChainId.ETH_RIN, address = "address3", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ETH_RIN, address = "address1", _isTestNetwork = true, isHide = false),
                Account(
                    4,
                    chainId = ChainId.ATS_SIGMA,
                    address = "address4",
                    _isTestNetwork = true,
                    isHide = true,
                    isDeleted = true
                ),
                Account(5, chainId = Int.InvalidValue, address = "address5", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ETH_RIN, address = "address6", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ATS_SIGMA, address = "address6", _isTestNetwork = true, isHide = true)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.getNumberOfAccountsToUse() shouldBeEqualTo 6
    }

    @Test
    fun `all main network accounts are not empty test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, chainId = ChainId.ETH_MAIN, _isTestNetwork = false),
                Account(1, _isTestNetwork = true),
                Account(1, _isTestNetwork = false),
                Account(4, _isTestNetwork = false)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.areAllEmptyMainNetworkAccounts() shouldBeEqualTo false
    }

    @Test
    fun `all main network accounts are empty test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, _isTestNetwork = true),
                Account(1, _isTestNetwork = true),
                Account(2, _isTestNetwork = true),
                Account(3, _isTestNetwork = true),
                Account(1, _isTestNetwork = false),
                Account(4, _isTestNetwork = false),
                Account(5, _isTestNetwork = false)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.areAllEmptyMainNetworkAccounts() shouldBeEqualTo true
    }

    @Test
    fun `change account name test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(0, chainId = 1, name = "#1 account")
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                account,
                Account(1, chainId = 1, name = "#2 account")
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.changeAccountName(account, "new name")
        verify(walletConfigManager, times(1)).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(0, chainId = 1, name = "#1 new name"),
                    Account(1, chainId = 1, name = "#2 account")
                )
            )
        )
    }

    @Test
    fun `get first active account test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
                Account(2, chainId = ChainId.ATS_TAU, address = "address2", _isTestNetwork = true, isHide = false),
                Account(3, chainId = ChainId.ETH_RIN, address = "address3", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ETH_RIN, address = "address1", _isTestNetwork = true, isHide = false),
                Account(
                    4,
                    chainId = ChainId.ATS_SIGMA,
                    address = "address4",
                    _isTestNetwork = true,
                    isHide = true,
                    isDeleted = true
                ),
                Account(5, chainId = Int.InvalidValue, address = "address5", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ETH_RIN, address = "address6", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ATS_SIGMA, address = "address6", _isTestNetwork = true, isHide = true)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.getFirstActiveAccountOrNull(ChainId.ATS_TAU) shouldBeEqualTo
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false)
        manager.getFirstActiveAccountOrNull(ChainId.ATS_SIGMA) shouldBeEqualTo null
    }

    @Test
    fun `get all first active accounts test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
                Account(2, chainId = ChainId.ATS_TAU, address = "address2", _isTestNetwork = true, isHide = false),
                Account(3, chainId = ChainId.ETH_RIN, address = "address3", _isTestNetwork = true, isHide = true),
                Account(1, chainId = ChainId.ETH_RIN, address = "address1", _isTestNetwork = true, isHide = false),
                Account(
                    4,
                    chainId = ChainId.ATS_SIGMA,
                    address = "address4",
                    _isTestNetwork = true,
                    isHide = true,
                    isDeleted = true
                ),
                Account(5, chainId = Int.InvalidValue, address = "address5", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ETH_RIN, address = "address6", _isTestNetwork = true, isHide = false),
                Account(6, chainId = ChainId.ATS_SIGMA, address = "address6", _isTestNetwork = true, isHide = true)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.getFirstActiveAccountForAllNetworks() shouldBeEqualTo listOf(
            Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
            Account(1, chainId = ChainId.ETH_RIN, address = "address1", _isTestNetwork = true, isHide = false)
        )
    }

    @Test
    fun `succesfull hide accounts test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = false)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                account,
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
                Account(0, chainId = ChainId.ETH_RIN, address = "address2", _isTestNetwork = true, isHide = false)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        manager.hideAccount(account)
        verify(walletConfigManager).updateWalletConfig(
            WalletConfig(
                2, accounts = listOf(
                    Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = true),
                    Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
                    Account(0, chainId = ChainId.ETH_RIN, address = "address2", _isTestNetwork = true, isHide = false)
                )
            )
        )
    }

    @Test
    fun `error hide accounts test`() {
        NetworkManager.initialize(MockDataProvider.networks)
        val account = Account(0, chainId = ChainId.ATS_TAU, address = "address0", _isTestNetwork = true, isHide = false)
        val walletConfig = WalletConfig(
            1, accounts = listOf(
                Account(1, chainId = ChainId.ATS_TAU, address = "address1", _isTestNetwork = true, isHide = false),
                Account(0, chainId = ChainId.ETH_RIN, address = "address2", _isTestNetwork = true, isHide = false)
            )
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
        val result = manager.hideAccount(account)
        result.blockingGet() shouldBeInstanceOf MissingAccountThrowable::class
    }
}