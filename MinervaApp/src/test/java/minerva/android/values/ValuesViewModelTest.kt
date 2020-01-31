package minerva.android.values

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.WalletConfig
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ValuesViewModelTest {

    private val walletManager: WalletManager = mock()
    private val viewModel = ValuesViewModel(walletManager)

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
    fun `Remove value success`() {
        whenever(walletManager.removeValue(any())).thenReturn(Completable.complete())
        val test = walletManager.removeValue(any()).test()
        test.assertNoErrors()
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(walletManager.removeValue(any())).thenReturn(Completable.error(error))
        val test = walletManager.removeValue(any()).test()
        test.assertError(error)
    }
}