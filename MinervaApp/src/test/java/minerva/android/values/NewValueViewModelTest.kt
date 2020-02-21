package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.create.NewValueViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterKey
import org.junit.Test

class NewValueViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = NewValueViewModel(walletManager, walletActionsRepository)

    private val saveWalletActionObserver: Observer<Event<Unit>> = mock()
    private val saveWalletActionCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `save wallet action success`() {
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveWalletAction()
            saveWalletActionCaptor.run {
                verify(saveWalletActionObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveWalletAction()
            viewModel.saveErrorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}