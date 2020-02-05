package minerva.android.onboarding.create

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.observeWithPredicate
import minerva.android.walletmanager.manager.WalletManager
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqualTo
import org.junit.Test

class CreateWalletViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val viewModel = CreateWalletViewModel(walletManager)

    private val loadingDialogObserver: Observer<Event<Boolean>> = mock()
    private val loadingDialogCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    @Test
    fun `create master seed should return success`() {
        walletManager.createMasterKeys { _, _, _ ->
            whenever(walletManager.createWalletConfig(any())).doReturn(Completable.complete())
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
            whenever(walletManager.createWalletConfig(any())).doReturn(Completable.error(error))
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