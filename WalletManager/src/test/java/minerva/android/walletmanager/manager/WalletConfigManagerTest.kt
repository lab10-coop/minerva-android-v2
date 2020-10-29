package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManagerImpl
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.utils.DataProvider.walletConfig
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class WalletConfigManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val walletConfigRepository: WalletConfigRepository = mock()

    private val walletManager =
        WalletConfigManagerImpl(keyStoreRepository, walletConfigRepository)

    private val walletConfigObserver: Observer<WalletConfig> = mock()
    private val walletConfigCaptor: KArgumentCaptor<WalletConfig> = argumentCaptor()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that loading wallet config returns success`() {
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver, times(1)).onChanged(capture())
            firstValue.identities[0].name shouldBeEqualTo "identityName1"
        }
    }

    @Test
    fun `Create default walletConfig should return success`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertComplete()
    }

    @Test
    fun `Create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(throwable))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `get wallet config success test`() {
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(walletConfigRepository.restoreWalletConfig(any())).thenReturn(Single.just(WalletConfigResponse(_message = "success")))
        walletManager.initWalletConfig()
        walletManager.restoreWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertComplete()
            .assertValue {
                it.message == "success"
            }
    }

    @Test
    fun `get wallet config error test`() {
        val error = Throwable()
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(walletConfigRepository.restoreWalletConfig(any())).thenReturn(Single.error(error))
        walletManager.initWalletConfig()
        walletManager.restoreWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertError(error)
    }

    @Test
    fun `save service test`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.saveService(Service())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save service test error`() {
        val error = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(error))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.saveService(Service()).test().assertComplete()
    }

    @Test
    fun `get value test`() {
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.apply {
            initWalletConfig()
            val result = getAccount(2)
            assertEquals(result?.index, 2)
        }
    }

    @Test
    fun `get safe account master owner key test`() {
        val expected = Account(0, address = "address", privateKey = "key")
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(WalletConfig(0, accounts = listOf(expected))))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.run {
            initWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "key")
        }
    }

    @Test
    fun `get safe account master owner key error test`() {
        val expected = Account(0, address = "123", privateKey = "key")
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(WalletConfig(0, accounts = listOf(expected))))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.run {
            initWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "")
        }
    }

    @Test
    fun `update safe account owners test`(){
        whenever(walletConfigRepository.updateWalletConfig(any(), any())) doReturn Completable.complete()
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.apply {
            initWalletConfig()
            updateSafeAccountOwners(0, listOf("owner"))
                .test()
                .assertNoErrors()
                .assertComplete()
        }
    }

    @Test
    fun `update safe account owners error test`(){
        val error = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())) doReturn Completable.error(error)
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.apply {
            initWalletConfig()
            updateSafeAccountOwners(0, listOf("owner"))
                .test()
                .assertComplete()
        }
    }
}