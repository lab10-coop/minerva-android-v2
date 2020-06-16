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
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.walletmanager.manager.identity.IdentityManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.utils.DataProvider
import minerva.android.walletmanager.utils.DataProvider.walletConfig
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IdentityManagerTest {

    private val walletConfigManager: WalletConfigManager = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val repository = IdentityManagerImpl(walletConfigManager, cryptographyRepository)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigManager.getWalletConfig()) doReturn walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that wallet manager returns correct value`() {
        repository.loadIdentity(0, "Identity").apply {
            index shouldBeEqualTo 0
            name shouldBeEqualTo "identityName1"
            privateKey shouldBeEqualTo "privateKey"
        }
        repository.loadIdentity(3, "Identity").apply {
            index shouldBeEqualTo walletConfig.newIndex
            name shouldBeEqualTo "Identity #8"
        }
        repository.loadIdentity(-1, "Identity").apply {
            index shouldBeEqualTo walletConfig.newIndex
            name shouldBeEqualTo "Identity #8"
        }
    }

    @Test
    fun `Check that wallet manager saves new identity`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val newIdentity = Identity(0, "identityName1")
        val test = repository.saveIdentity(newIdentity).test()
        val loadedIdentity = repository.loadIdentity(0, "Identity")
        test.assertNoErrors()
        loadedIdentity.name shouldBeEqualTo newIdentity.name
        loadedIdentity.privateKey shouldBeEqualTo "privateKey"
    }

    @Test
    fun `Check that wallet manager doesn't save when server error`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val newIdentity = Identity(0, "identityName")
        repository.saveIdentity(newIdentity).test()
        val loadedIdentity = repository.loadIdentity(0, "Identity")
        loadedIdentity.name shouldNotBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager removes correct identity`() {
        val identityToRemove = Identity(0, "identityName2", "", "privateKey", DataProvider.data)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeIdentity(identityToRemove).test()
        val loadedIdentity = repository.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo identityToRemove.name
        loadedIdentity.isDeleted shouldBeEqualTo identityToRemove.isDeleted
    }

    @Test
    fun `Check that wallet manager doesn't remove identity when server error`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeIdentity(identityToRemove).test()
        val loadedIdentity = repository.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName2"
        loadedIdentity.isDeleted shouldBeEqualTo false
        loadedIdentity.data.size shouldBeEqualTo 3
    }

    @Test
    fun `Check that wallet manager doesn't remove identity, when there is only one active element`() {
        val identityToRemove = Identity(0)
        val walletConfig = WalletConfig(
            0, listOf(
                Identity(0, "identityName1", "", "privateKey", DataProvider.data),
                Identity(1, "identityName1", "", "privateKey", DataProvider.data, isDeleted = true)
            )
        )
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeIdentity(identityToRemove).test()
        repository.loadIdentity(0, "Identity").apply {
            name shouldBeEqualTo "identityName1"
            isDeleted shouldBeEqualTo false
            data.size shouldBeEqualTo 3
        }
        walletConfig.identities.size shouldBeEqualTo 2
    }

    @Test
    fun `Check that wallet manager will not remove, when try to remove identity with wrong index`() {
        val identityToRemove = Identity(22)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.removeIdentity(identityToRemove).test()
        repository.loadIdentity(0, "Identity").apply {
            name shouldBeEqualTo "identityName1"
            isDeleted shouldBeEqualTo false
            data.size shouldBeEqualTo 3
        }
        walletConfig.identities.size shouldBeEqualTo 3
    }
}