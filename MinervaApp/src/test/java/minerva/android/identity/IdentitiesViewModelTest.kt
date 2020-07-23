package minerva.android.identity

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.IdentitiesViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class IdentitiesViewModelTest : BaseViewModelTest() {

    private val identityManager: IdentityManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = IdentitiesViewModel(identityManager, walletActionsRepository)

    @Test
    fun `remove identity error`(){
        val error = Throwable()
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.removeIdentity(Identity(1))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `remove identity success`() {
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.removeIdentity(Identity(1))
        viewModel.identityRemovedLiveData.observeLiveDataEvent(Event(Unit))
    }
}