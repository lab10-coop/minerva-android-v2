package minerva.android.services

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Before
import org.junit.Test

class ServicesViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val servicesManager: ServiceManager = mock()
    private val walletConnectRepository: WalletConnectRepository = mock()
    private lateinit var viewModel: ServicesViewModel

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val dappSessionObserver: Observer<List<MinervaPrimitive>> = mock()
    private val dappSessionCaptor: KArgumentCaptor<List<MinervaPrimitive>> = argumentCaptor()

    private val removeDappObserver: Observer<Event<Unit>> = mock()
    private val removeDappCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Before
    fun setup() {
        viewModel = ServicesViewModel(servicesManager, walletActionsRepository, walletConnectRepository)
    }

    @Test
    fun `remove service error test`() {
        val error = Throwable()
        whenever(servicesManager.removeService(any())) doReturn Completable.error(error)
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.complete()
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.removeService("1", "name")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove service success`() {
        whenever(servicesManager.removeService(any())) doReturn Completable.complete()
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.complete()
        viewModel.removeService("1", "name")
        viewModel.serviceRemovedLiveData.observeLiveDataEvent(Event(Unit))
    }

    @Test
    fun `set dapp session flowable`() {
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession(name = ""))))
        viewModel.setDappSessionsFlowable(listOf(Service(name = "name")))
        viewModel.dappSessionsLiveData.observeForever(dappSessionObserver)
        dappSessionCaptor.run {
            verify(dappSessionObserver).onChanged(capture())
        }
    }

    @Test
    fun `set dapp session flowable and error occurs`() {
        val error = Throwable()
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.error(error))
        viewModel.setDappSessionsFlowable(listOf(Service(name = "name")))
        viewModel.errorLiveData.observeForever(errorObserver)
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove session test`() {
        whenever(walletConnectRepository.killSession(any())).thenReturn(Completable.complete())
        viewModel.removeSession(DappSession(peerId = "peerId"))
        verify(walletConnectRepository).killSession("peerId")
    }
}