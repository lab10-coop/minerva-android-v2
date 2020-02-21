package minerva.android.identity

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.edit.EditIdentityViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterKey
import org.junit.Test

class EditIdentityViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = EditIdentityViewModel(walletManager, walletActionsRepository)

    private val saveWalletActionObserver: Observer<Event<Unit>> = mock()
    private val saveWalletActionCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `save wallet action success`() {
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveWalletAction(1)
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
            saveWalletAction(1)
            saveErrorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}