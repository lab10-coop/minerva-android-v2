package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.create.NewValueViewModel
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import org.junit.Test

class NewValueViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = NewValueViewModel(walletManager, walletActionsRepository)

    private val createValueObserver: Observer<Event<Unit>> = mock()
    private val createValueCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `create wallet action success`() {
        whenever(walletManager.createValue(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterSeed).thenReturn(MasterSeed("", "", ""))
        viewModel.createValueLiveData.observeForever(createValueObserver)
        viewModel.createNewValue(Network.ARTIS, 1)
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(walletManager.createValue(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterSeed).thenReturn(MasterSeed("12", "34", "21"))
        viewModel.createNewValue(Network.ARTIS, 1)
        viewModel.saveErrorLiveData.observeLiveDataEvent(Event(error))
    }
}