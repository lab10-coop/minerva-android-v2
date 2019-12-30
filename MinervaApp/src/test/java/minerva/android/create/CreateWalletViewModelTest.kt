package minerva.android.create

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.observeWithPredicate
import minerva.android.onboarding.create.CreateWalletViewModel
import minerva.android.walletmanager.manager.WalletManager
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateWalletViewModelTest {

    private val walletManager: WalletManager = mock()
    private val viewModel = CreateWalletViewModel(walletManager)

    private val loadingDialogObserver: Observer<Event<Boolean>> = mock()
    private val loadingDialogCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

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
    fun `create master seed should return success`() {
        walletManager.createMasterKeys { _, _, _ ->
            whenever(walletManager.createDefaultWalletConfig(any())).doReturn(Completable.complete())
            viewModel.loadingLiveData.observeForever(loadingDialogObserver)
            viewModel.createMasterSeed()
            checkLoadingDialogLiveData()
            viewModel.createWalletLiveData.observeWithPredicate { it.peekContent() == Unit }
        }
    }

    @Test
    fun `create master seed should return error`() {
        val error = Throwable()
        walletManager.createMasterKeys { _, _, _ ->
            whenever(walletManager.createDefaultWalletConfig(any())).doReturn(Completable.error(error))
            viewModel.loadingLiveData.observeForever(loadingDialogObserver)
            viewModel.createMasterSeed()
            checkLoadingDialogLiveData()
            viewModel.errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    private fun checkLoadingDialogLiveData() {
        loadingDialogCaptor.run {
            verify(loadingDialogObserver, times(2)).onChanged(capture())
            firstValue shouldBeInstanceOf Event::class
            firstValue.peekContent() shouldEqualTo true
            secondValue shouldBeInstanceOf Event::class
            secondValue.peekContent() shouldEqualTo false
        }
        verifyNoMoreInteractions(loadingDialogObserver)
    }
}