package minerva.android.identity

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.identities.IdentitiesViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterSeed
import org.junit.Test

class IdentitiesViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = IdentitiesViewModel(walletManager, walletActionsRepository)

    @Test
    fun `remove identity success error`(){
        val error = Throwable()
        whenever(walletManager.removeIdentity(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterSeed).thenReturn(MasterSeed("", ""))
        viewModel.removeIdentity(Identity(1))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }
}