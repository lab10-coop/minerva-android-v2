package minerva.android.walletmanager.manager.accounts

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.utils.RxTest
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
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
        whenever(walletConfigManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `Check that wallet manager creates new regular account`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address1")
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
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
        val network = Network(short = "eth_rinkeby", httpRpc = "some")
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn(String.Empty)
        whenever(
            cryptographyRepository.calculateDerivedKeys(
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
    fun `Check that wallet manager removes correct empty value`() {
        val account = Account(2)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any())).thenReturn(
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
        val account = Account(1)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
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
            cryptographyRepository.calculateDerivedKeys(
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
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
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
        NetworkManager.initialize(DataProvider.networks)
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("address")
        manager.createSafeAccount(Account(1, networkShort = "eth_rinkeby"), "contract")
            .test()
            .assertComplete()
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("address")
        manager.createSafeAccount(Account(1, networkShort = "eth_rinkeby"), "contract")
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
    fun `toggle main nets enabled returns true`() {
        whenever(walletConfigManager.toggleMainNetsEnabled).thenReturn(true)
        val result = manager.toggleMainNetsEnabled
        assertEquals(true, result)
    }

    @Test
    fun `enable main nets emits true`() {
        whenever(walletConfigManager.enableMainNetsFlowable).thenReturn(Flowable.just(true))
        manager.enableMainNetsFlowable.test().assertValue { it }
    }

    @Test
    fun `enable main nets emits false`() {
        whenever(walletConfigManager.enableMainNetsFlowable).thenReturn(Flowable.just(false))
        manager.enableMainNetsFlowable.test().assertValue { !it }
    }

    @Test
    fun `get token visibility test`() {
        whenever(localStorage.getTokenVisibilitySettings()).thenReturn(TokenVisibilitySettings())
        manager.getTokenVisibilitySettings()
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
        assertEquals(result?.get(0)?.id, 1)
    }

    @Test
    fun `to checksum address test`() {
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("checksum")
        val result = manager.toChecksumAddress("address")
        assertEquals(result, "checksum")
    }
}