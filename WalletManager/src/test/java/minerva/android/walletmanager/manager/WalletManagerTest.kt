package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WalletManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val walletConfigRepository: WalletConfigRepository = mock()

    private val walletManager = WalletManagerImpl(keyStoreRepository, cryptographyRepository, walletConfigRepository)

    private val walletConfig = WalletConfig(0, listOf(Identity(0, "walletConfigName", "", "name")))

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
        walletManager.loadWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver, times(1)).onChanged(capture())
            firstValue.identities[0].name shouldBeEqualTo "walletConfigName"
        }
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(walletConfigRepository.createDefaultWalletConfig(any())).thenReturn(Completable.complete())
        val test = walletManager.createDefaultWalletConfig(MasterKey("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(walletConfigRepository.createDefaultWalletConfig(any())).thenReturn(Completable.error(throwable))
        val test = walletManager.createDefaultWalletConfig(MasterKey("1234", "5678")).test()
        test.assertError(throwable)
    }
}