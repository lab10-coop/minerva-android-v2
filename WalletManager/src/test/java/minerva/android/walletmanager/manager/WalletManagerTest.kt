package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.Identity
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
    private val api: MinervaApi = mock()

    private val walletManager = WalletManagerImpl(keyStoreRepository, cryptographyRepository, walletConfigRepository, api)

    private val walletConfig = WalletConfig(listOf(Identity(0, "", "", "name")))

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
        whenever(walletConfigRepository.loadWalletConfig()).thenReturn(Observable.just(walletConfig))
        walletManager.loadWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver, times(1)).onChanged(capture())
            firstValue.identities[0].identityName shouldBeEqualTo "name"
        }
    }
}