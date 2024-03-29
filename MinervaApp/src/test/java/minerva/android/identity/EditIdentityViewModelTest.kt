package minerva.android.identity

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.edit.EditIdentityViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class EditIdentityViewModelTest : BaseViewModelTest() {

    private val identityManager: IdentityManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = EditIdentityViewModel(identityManager, walletActionsRepository)

    private val saveIdentityObserver: Observer<Event<Identity>> = mock()
    private val saveIdentityCaptor: KArgumentCaptor<Event<Identity>> = argumentCaptor()

    @Test
    fun `save identity success test`() {
        whenever(identityManager.saveIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.apply {
            saveCompletedLiveData.observeForever(saveIdentityObserver)
            saveIdentity(Identity(1), ADDED)
            saveIdentityCaptor.run {
                verify(saveIdentityObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `save identity error test`() {
        val error = Throwable()
        whenever(identityManager.saveIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.saveIdentity(Identity(1), ADDED)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}