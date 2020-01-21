package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainProvider
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WalletManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val walletConfigRepository: WalletConfigRepository = mock()
    private val blockchainProvider: BlockchainProvider = mock()
    private val localStorage: LocalStorage = mock()
    private val servicesApi: ServicesApi = mock()

    private val walletManager =
        WalletManagerImpl(keyStoreRepository, cryptographyRepository, walletConfigRepository, blockchainProvider, localStorage, servicesApi)

    private val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    private val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "", "privateKey", data),
            Identity(1, "identityName2", "", "privateKey", data),
            Identity(3, "identityName3", "", "privateKey", data)
        )
    )

    private val walletConfigObserver: Observer<WalletConfig> = mock()
    private val walletConfigCaptor: KArgumentCaptor<WalletConfig> = argumentCaptor()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that loading wallet config returns success`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver, times(1)).onChanged(capture())
            firstValue.identities[0].name shouldBeEqualTo "identityName1"
        }
    }

    @Test
    fun `Create default walletConfig should return success`() {
        whenever(walletConfigRepository.createWalletConfig(any())).thenReturn(Completable.complete())
        val test = walletManager.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `Create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(walletConfigRepository.createWalletConfig(any())).thenReturn(Completable.error(throwable))
        val test = walletManager.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `Check that wallet manager returns correct value`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        val identity = walletManager.loadIdentity(0, "Identity")
        identity.index shouldEqualTo 0
        identity.name shouldBeEqualTo "identityName1"
        identity.privateKey shouldBeEqualTo "privateKey"
        val identity3 = walletManager.loadIdentity(3, "Identity")
        identity3.index shouldEqualTo walletConfig.newIndex
        identity3.name shouldBeEqualTo "Identity #3"
        val identityMinusOne = walletManager.loadIdentity(-1, "Identity")
        identityMinusOne.index shouldEqualTo walletConfig.newIndex
        identityMinusOne.name shouldBeEqualTo "Identity #3"
    }

    @Test
    fun `Check that wallet manager saves new identity`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "newIdentity")
        val test = walletManager.saveIdentity(newIdentity).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        test.assertNoErrors()
        loadedIdentity.name shouldBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager doesn't save when server error`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "newIdentity")
        walletManager.saveIdentity(newIdentity).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldNotBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager removes correct element`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo identityToRemove.name
        loadedIdentity.isDeleted shouldEqualTo true
        loadedIdentity.data.size shouldEqualTo identityToRemove.data.size
    }

    @Test
    fun `Check that wallet manager doesn't remove identity when server error`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName2"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
    }

    @Test
    fun `Check that wallet manager doesn't remove identity, when there is only one active element`() {
        val identityToRemove = Identity(0)
        val walletConfig = WalletConfig(
            0, listOf(
                Identity(0, "identityName1", "", "privateKey", data),
                Identity(1, "identityName1", "", "privateKey", data, isDeleted = true)
            )
        )
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
        walletConfig.identities.size shouldEqualTo 2
    }

    @Test
    fun `Check that wallet manager will not remove, when try to remove identity with wrong index`() {
        val identityToRemove = Identity(22)
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
        walletConfig.identities.size shouldEqualTo 3
    }
}