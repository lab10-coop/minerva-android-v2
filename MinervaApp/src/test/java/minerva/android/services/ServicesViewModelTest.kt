package minerva.android.services

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Before
import org.junit.Test

class ServicesViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val servicesManager: ServiceManager = mock()
    private lateinit var viewModel: ServicesViewModel

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Before
    fun setup() {
        viewModel = ServicesViewModel(servicesManager, walletActionsRepository)
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
}