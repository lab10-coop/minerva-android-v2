package minerva.android.onboarding.create

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeWithPredicate
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test

class CreateWalletViewModelTest : BaseViewModelTest() {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = CreateWalletViewModel(masterSeedRepository)

    private val loadingDialogObserver: Observer<Event<Boolean>> = mock()
    private val loadingDialogCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    @Test
    fun `create master seed should return success`() {
        whenever(masterSeedRepository.createWalletConfig()).doReturn(Completable.complete())
        viewModel.loadingLiveData.observeForever(loadingDialogObserver)
        viewModel.createWalletConfig()
        checkLoadingDialogLiveData()
        viewModel.createWalletLiveData.observeWithPredicate { it.peekContent() == Unit }
    }

    @Test
    fun `create master seed should return error`() {
        val error = Throwable()
        whenever(masterSeedRepository.createWalletConfig()).doReturn(Completable.error(error))
        viewModel.loadingLiveData.observeForever(loadingDialogObserver)
        viewModel.createWalletConfig()
        checkLoadingDialogLiveData()
        viewModel.createWalletLiveData.observeWithPredicate { it.peekContent() == Unit }
    }

    private fun checkLoadingDialogLiveData() {
        loadingDialogCaptor.run {
            verify(loadingDialogObserver, times(2)).onChanged(capture())
            firstValue shouldBeInstanceOf Event::class
            firstValue.peekContent() shouldBeEqualTo true
            secondValue shouldBeInstanceOf Event::class
            secondValue.peekContent() shouldBeEqualTo false
        }
        verifyNoMoreInteractions(loadingDialogObserver)
    }
}