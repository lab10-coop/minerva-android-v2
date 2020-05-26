package minerva.android.identity

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.edit.EditIdentityViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import org.junit.Test

class EditIdentityViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = EditIdentityViewModel(walletManager, walletActionsRepository)

    private val saveIdentityObserver: Observer<Event<Unit>> = mock()
    private val saveIdentityCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `save identity success`() {
        whenever(walletManager.saveIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterSeed).thenReturn(MasterSeed("12", "34"))
        viewModel.apply {
            saveCompletedLiveData.observeForever(saveIdentityObserver)
            saveIdentity(Identity(1), ADDED)
            saveIdentityCaptor.run {
                verify(saveIdentityObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `save identity error`() {
        val error = Throwable()
        whenever(walletManager.saveIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterSeed).thenReturn(MasterSeed("12", "34"))
        viewModel.apply {
            saveIdentity(Identity(1), ADDED)
            saveErrorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}