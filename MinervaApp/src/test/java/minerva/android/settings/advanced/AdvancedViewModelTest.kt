package minerva.android.settings.advanced

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AdvancedViewModelTest : BaseViewModelTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val viewModel = AdvancedViewModel(walletConfigManager)

    private val resetTokensObserver: Observer<Event<Result<Any>>> = mock()
    private val resetTokensCaptor: KArgumentCaptor<Event<Result<Any>>> = argumentCaptor()

    @Test
    fun `reset tokens should success update live data`(){
        whenever(walletConfigManager.removeAllTokens()).thenReturn(Completable.complete())
        viewModel.resetTokens()
        viewModel.resetTokensLiveData.observeForever(resetTokensObserver)
        resetTokensCaptor.run {
            verify(resetTokensObserver).onChanged(capture())
            firstValue.peekContent().isSuccess shouldBeEqualTo true
        }
    }

    @Test
    fun `reset tokens should fail update live data`(){
        whenever(walletConfigManager.removeAllTokens()).thenReturn(Completable.error(Throwable()))
        viewModel.resetTokens()
        viewModel.resetTokensLiveData.observeForever(resetTokensObserver)
        resetTokensCaptor.run {
            verify(resetTokensObserver).onChanged(capture())
            firstValue.peekContent().isFailure shouldBeEqualTo true
        }
    }
}