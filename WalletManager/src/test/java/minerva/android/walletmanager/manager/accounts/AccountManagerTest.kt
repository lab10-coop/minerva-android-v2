package minerva.android.walletmanager.manager.accounts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.RxTest
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class AccountManagerTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val blockchainRepository: BlockchainRepository = mock()
    private val repository = AccountManagerImpl(walletConfigManager, cryptographyRepository, blockchainRepository)

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `Check that wallet manager saves new Value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val test = repository.createAccount(Network.ETH_RIN, "#3 Ethereum").test()
        test.assertNoErrors()
        repository.loadAccount(1).apply {
            index shouldBeEqualTo 4
            publicKey shouldBeEqualTo "publicKey2"
            privateKey shouldBeEqualTo "privateKey2"
            address shouldBeEqualTo "address"
        }
    }

    @Test
    fun `Check that wallet manager don't save new value when server error`() {
        val error = Throwable()
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val test = repository.createAccount(Network.ETH_RIN, "#3 Ethereum").test()
        test.assertError(error)
        repository.loadAccount(10).apply {
            index shouldBeEqualTo -1
            privateKey shouldBeEqualTo String.Empty
            publicKey shouldBeEqualTo String.Empty
            address shouldBeEqualTo String.Empty
        }
    }

    @Test
    fun `Check that wallet manager removes correct empty value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        repository.removeAccount(2).test()
        val removedValue = repository.loadAccount(0)
        val notRemovedValue = repository.loadAccount(1)
        removedValue.index shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.index shouldBeEqualTo 4
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct not empty value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(300))
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeAccount(2).test()
        val removedValue = repository.loadAccount(0)
        val notRemovedValue = repository.loadAccount(1)
        removedValue.index shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.index shouldBeEqualTo 4
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager don't removes correct safe account value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        repository.removeAccount(5).test()
        repository.removeAccount(6).test()
        repository.loadAccount(2).apply {
            index shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadAccount(3).apply {
            index shouldBeEqualTo 6
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `Check that wallet manager removes correct safe account value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeAccount(5).test()
        repository.removeAccount(6).test()
        repository.loadAccount(2).apply {
            index shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadAccount(3).apply {
            index shouldBeEqualTo 6
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `get safe account number test`() {
        val expected = Account(0, address = "123", privateKey = "key", owners = listOf("owner"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(accounts = listOf(expected))
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 2
        repository.run {
            val result = getSafeAccountCount("owner")
            assertEquals(result, 2)
        }
    }

    @Test
    fun `get safe account number error test`() {
        val expected = Account(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(accounts = listOf(expected))
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 1
        repository.run {
            val result = getSafeAccountCount("owner")
            assertEquals(result, 1)
        }
    }
}