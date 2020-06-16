package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.create.NewValueViewModel
import minerva.android.walletmanager.manager.values.ValueManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class NewValueViewModelTest : BaseViewModelTest() {

    private val valueManager: ValueManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = NewValueViewModel(valueManager, walletActionsRepository)

    private val createValueObserver: Observer<Event<Unit>> = mock()
    private val createValueCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `create wallet action success`() {
        whenever(valueManager.createValue(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.createValueLiveData.observeForever(createValueObserver)
        viewModel.createNewValue(Network.ARTIS, 1)
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(valueManager.createValue(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.createNewValue(Network.ARTIS, 1)
        viewModel.saveErrorLiveData.observeLiveDataEvent(Event(error))
    }
}