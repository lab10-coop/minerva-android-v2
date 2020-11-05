package minerva.android.identity

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.MinervaPrimitivesViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class MinervaPrimitivesViewModelTest : BaseViewModelTest() {

    private val identityManager: IdentityManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel =
        MinervaPrimitivesViewModel(identityManager, walletActionsRepository)

    private val removeCredentialObserver: Observer<Event<Any>> = mock()
    private val removeCredentialCaptor: KArgumentCaptor<Event<Any>> = argumentCaptor()

    private val removeIdentityObserver: Observer<Event<Unit>> = mock()
    private val removeIdentityCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `remove identity success test`() {
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.identityRemovedLiveData.observeForever(removeIdentityObserver)
        viewModel.removeIdentity(Identity(1))
        removeIdentityCaptor.run {
            verify(removeIdentityObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove identity error test`() {
        val error = Throwable()
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.removeIdentity(Identity(1))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `remove credential success test`() {
        whenever(identityManager.removeBindedCredentialFromIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.removeCredentialLiveData.observeForever(removeCredentialObserver)
        viewModel.removeCredential(Credential("name"))
        removeCredentialCaptor.run {
            verify(removeCredentialObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove credential error test`() {
        val error = Throwable()
        whenever(identityManager.removeBindedCredentialFromIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.removeCredential(Credential("name"))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}