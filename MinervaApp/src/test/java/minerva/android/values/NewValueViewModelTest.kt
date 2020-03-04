package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.InvalidVersion
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.create.NewValueViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfig
import org.junit.Test

class NewValueViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = NewValueViewModel(walletManager, walletActionsRepository)

    private val createValueObserver: Observer<Event<Unit>> = mock()
    private val createValueCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `create wallet action success`() {
        whenever(walletManager.createValue(any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.createValueLiveData.observeForever(createValueObserver)
        viewModel.createNewValue(Network.ARTIS, 1)
        createValueCaptor.run {
            verify(createValueObserver).onChanged(capture())
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(walletManager.createValue(any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.createNewValue(Network.ARTIS, 1)
        viewModel.saveErrorLiveData.observeLiveDataEvent(Event(error))
    }
}