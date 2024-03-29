package minerva.android.actions

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletActions.WalletActionsViewModel
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.wallet.WalletActionClustered
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class WalletActionsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = WalletActionsViewModel(walletActionsRepository)
    private val actions = mutableListOf(WalletActionClustered(1L, mutableListOf(WalletAction(1, 2, 1234L, hashMapOf()))))

    private val walletActionsObserver: Observer<Event<List<WalletActionClustered>>> = mock()
    private val walletActionsCaptor: KArgumentCaptor<Event<List<WalletActionClustered>>> = argumentCaptor()

    @Test
    fun `test fetch wallet actions success`() {
        whenever(walletActionsRepository.getWalletActions()).thenReturn(Observable.just(actions))
        viewModel.walletActionsLiveData.observeForever(walletActionsObserver)
        viewModel.fetchWalletActions()
        walletActionsCaptor.run {
            verify(walletActionsObserver).onChanged(capture())
            firstValue.peekContent()[0].walletActions[0].status == 2
        }
    }

    @Test
    fun `test fetch wallet actions error`() {
        val error = Throwable()
        whenever(walletActionsRepository.getWalletActions()).thenReturn(Observable.error(error))
        viewModel.walletActionsLiveData.observeForever(walletActionsObserver)
        viewModel.fetchWalletActions()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}