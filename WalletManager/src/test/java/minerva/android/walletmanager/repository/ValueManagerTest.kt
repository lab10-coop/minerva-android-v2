package minerva.android.walletmanager.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.values.ValueManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class ValueManagerTest {

    private val walletConfigManager: WalletConfigManager = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val blockchainRepository: BlockchainRepository = mock()
    private val repository = ValueManagerImpl(walletConfigManager, cryptographyRepository, blockchainRepository)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that wallet manager saves new Value`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val test = repository.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertNoErrors()
        repository.loadValue(1).apply {
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
        val test = repository.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertError(error)
        repository.loadValue(10).apply {
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
        repository.removeValue(2).test()
        val removedValue = repository.loadValue(0)
        val notRemovedValue = repository.loadValue(1)
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
        repository.removeValue(2).test()
        val removedValue = repository.loadValue(0)
        val notRemovedValue = repository.loadValue(1)
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
        repository.removeValue(5).test()
        repository.removeValue(6).test()
        repository.loadValue(2).apply {
            index shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadValue(3).apply {
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
        repository.removeValue(5).test()
        repository.removeValue(6).test()
        repository.loadValue(2).apply {
            index shouldBeEqualTo 5
            publicKey shouldBeEqualTo "publicKey3"
            isDeleted shouldBeEqualTo false
        }
        repository.loadValue(3).apply {
            index shouldBeEqualTo 6
            publicKey shouldBeEqualTo "publicKey4"
            isDeleted shouldBeEqualTo false
        }
    }

    @Test
    fun `get safe account number test`() {
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("owner"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(values = listOf(expected))
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 2
        repository.run {
            val result = getSafeAccountNumber("owner")
            assertEquals(result, 2)
        }
    }

    @Test
    fun `get safe account number error test`() {
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(values = listOf(expected))
        whenever(walletConfigManager.getSafeAccountNumber(any())) doReturn 1
        repository.run {
            val result = getSafeAccountNumber("owner")
            assertEquals(result, 1)
        }
    }
}