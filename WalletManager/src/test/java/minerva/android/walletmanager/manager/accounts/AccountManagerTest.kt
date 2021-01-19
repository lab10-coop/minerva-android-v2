package minerva.android.walletmanager.manager.accounts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.RxTest
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
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
    private val blockchainRegularAccountRepository: BlockchainRegularAccountRepository = mock()
    private val repository = AccountManagerImpl(
        walletConfigManager,
        cryptographyRepository,
        blockchainRegularAccountRepository
    )

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `Check that wallet manager creates new regular account`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address")
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
        val test = repository.createRegularAccount(Network()).test()
        test.assertNoErrors()
        repository.loadAccount(1).apply {
            id shouldBeEqualTo 4
            publicKey shouldBeEqualTo "publicKey2"
            privateKey shouldBeEqualTo "privateKey2"
            address shouldBeEqualTo "address"
        }
    }

    @Test
    fun `Check that wallet manager don't save new regular account`() {
        val error = Throwable()
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
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
        val test = repository.createRegularAccount(Network()).test()
        test.assertError(error)
        repository.loadAccount(10).apply {
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
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address")
        repository.removeAccount(account).test()
        val removedValue = repository.loadAccount(0)
        val notRemovedValue = repository.loadAccount(1)
        removedValue.id shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.id shouldBeEqualTo 4
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct not empty value`() {
        val account = Account(2)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
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
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(300))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address")
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeAccount(account).test()
        val removedValue = repository.loadAccount(0)
        val notRemovedValue = repository.loadAccount(1)
        removedValue.id shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.id shouldBeEqualTo 4
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
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address")
        repository.removeAccount(account).test()
        repository.removeAccount(account2).test()
        repository.loadAccount(2).apply {
            id shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadAccount(3).apply {
            id shouldBeEqualTo 6
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `Check that wallet manager removes correct safe account value`() {
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
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRegularAccountRepository.toGwei(any())).thenReturn(BigDecimal.valueOf(256))
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).doReturn("address")
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeAccount(account).test()
        repository.removeAccount(account2).test()
        repository.loadAccount(2).apply {
            id shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadAccount(3).apply {
            id shouldBeEqualTo 6
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
        repository.run {
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
        repository.run {
            val result = getSafeAccountCount("owner")
            assertEquals(result, 1)
        }
    }

    @Test
    fun `create safe account success`() {
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        repository.createSafeAccount(Account(1), "contract")
            .test()
            .assertComplete()
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        whenever(cryptographyRepository.calculateDerivedKeys(any(), any(), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        repository.createSafeAccount(Account(1), "contract")
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account name test`() {
        val result =
            repository.getSafeAccountName(Account(1, address = "masterOwner", name = "test"))
        assertEquals("test", result)
    }

    @Test
    fun `is address valid success`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(true)
        val result = repository.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(false)
        val result = repository.isAddressValid("2342343")
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled returns true`() {
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(true)
        val result = repository.areMainNetworksEnabled
        assertEquals(true, result)
    }

    @Test
    fun `toggle main nets enabled returns true`() {
        whenever(walletConfigManager.toggleMainNetsEnabled).thenReturn(true)
        val result = repository.toggleMainNetsEnabled
        assertEquals(true, result)
    }

    @Test
    fun `enable main nets emits true`() {
        whenever(walletConfigManager.enableMainNetsFlowable).thenReturn(Flowable.just(true))
        repository.enableMainNetsFlowable.test().assertValue { it }
    }

    @Test
    fun `enable main nets emits false`() {
        whenever(walletConfigManager.enableMainNetsFlowable).thenReturn(Flowable.just(false))
        repository.enableMainNetsFlowable.test().assertValue { !it }
    }
}