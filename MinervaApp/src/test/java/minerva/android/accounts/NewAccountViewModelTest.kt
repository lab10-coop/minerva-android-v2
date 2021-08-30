package minerva.android.accounts

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.create.NewAccountViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Before
import org.junit.Test

class NewAccountViewModelTest : BaseViewModelTest() {

    private val accountManager: AccountManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private lateinit var viewModel: NewAccountViewModel

    private val createValueObserver: Observer<Event<Unit>> = mock()
    private val createValueCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Before
    fun `init view model`() {
        whenever(accountManager.createEmptyAccounts(any())).thenReturn(Completable.complete())
        whenever(accountManager.getAllAccountsForSelectedNetworksType()).thenReturn(
            listOf(Account(0), Account(1), Account(2), Account(3), Account(4))
        )
        viewModel = NewAccountViewModel(accountManager, walletActionsRepository)
    }

    @Test
    fun `should add empty accounts`() {
        whenever(accountManager.getAllAccountsForSelectedNetworksType()).thenReturn(emptyList())
        whenever(accountManager.createEmptyAccounts(any())).thenReturn(Completable.complete())
        viewModel.refreshAddressesLiveData.observeForever(createValueObserver)
        viewModel.shouldAddAccount()
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `should not add empty accounts`() {
        viewModel.shouldAddAccount()
        verify(accountManager, never()).createEmptyAccounts(any())
    }

    @Test
    fun `create wallet action success`() {
        whenever(accountManager.connectAccountToNetwork(any(), any())).thenReturn(Single.just("accountName"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.createAccountLiveData.observeForever(createValueObserver)
        viewModel.connectAccountToNetwork(1, Network())
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `create wallet action error`() {
        val error = Throwable()
        whenever(accountManager.connectAccountToNetwork(any(), any())).thenReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.connectAccountToNetwork(1, Network())
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}