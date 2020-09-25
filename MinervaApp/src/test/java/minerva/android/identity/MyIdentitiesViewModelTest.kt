package minerva.android.identity

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.myIdentities.MyIdentitiesViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class MyIdentitiesViewModelTest : BaseViewModelTest() {

    private val identityManager: IdentityManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel =
        MyIdentitiesViewModel(identityManager, walletActionsRepository)

    @Test
    fun `remove identity error`(){
        val error = Throwable()
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.error(error))
        viewModel.removeIdentity(Identity(1))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `remove identity success`() {
        val error = Throwable()
        whenever(identityManager.removeIdentity(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.removeIdentity(Identity(1))
        viewModel.identityRemovedLiveData.observeLiveDataEvent(Event(Unit))
    }

    @Test
    fun `remove credential error test`(){
        val error = Throwable()
        whenever(identityManager.removeBindedCredentialFromIdentity(any())).thenReturn(Completable.error(error))
        viewModel.removeCredential(Credential("name"))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}