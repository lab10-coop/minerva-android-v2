package minerva.android.accounts

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.accounts.create.NewAccountViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class NewAccountViewModelTest : BaseViewModelTest() {

    private val accountManager: AccountManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = NewAccountViewModel(accountManager, walletActionsRepository)

    private val createValueObserver: Observer<Event<Unit>> = mock()
    private val createValueCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `create wallet action success`() {
        whenever(accountManager.createAccount(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.createAccountLiveData.observeForever(createValueObserver)
        viewModel.createNewAccount(Network(), 1)
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(accountManager.createAccount(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.createNewAccount(Network(), 1)
        viewModel.saveErrorLiveData.observeLiveDataEvent(Event(error))
    }
}