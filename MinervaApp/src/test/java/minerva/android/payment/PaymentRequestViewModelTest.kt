package minerva.android.payment

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.Payment
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class PaymentRequestViewModelTest : BaseViewModelTest() {

    private val serviceManager: ServiceManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = PaymentRequestViewModel(serviceManager, walletActionsRepository, masterSeedRepository)

    private val showPaymentConfirmationObserver: Observer<Event<Unit>> = mock()
    private val showPaymentConfirmationCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val showConnectionRequestObserver: Observer<Event<String?>> = mock()
    private val showConnectionRequestCaptor: KArgumentCaptor<Event<String?>> = argumentCaptor()

    private val confirmPaymentObserver: Observer<Event<String>> = mock()
    private val confirmPaymentCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val newServiceObserver: Observer<Event<Unit>> = mock()
    private val newServiceCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `decode token success when service is not connected`() {
        whenever(serviceManager.decodePaymentRequestToken(any())).thenReturn(Single.just(Pair(Payment("12", serviceName = "test"), listOf())))
        viewModel.run {
            showConnectionRequestLiveData.observeForever(showConnectionRequestObserver)
            decodeJwtToken("token")
        }
        showConnectionRequestCaptor.run {
            verify(showConnectionRequestObserver).onChanged(capture())
            firstValue.peekContent() == "test"
        }
    }

    @Test
    fun `decode token success when service is connected`() {
        whenever(serviceManager.decodePaymentRequestToken(any())).thenReturn(
            Single.just(
                Pair(
                    Payment("12"),
                    listOf(Service(type = "1", name = "M27"))
                )
            )
        )
        viewModel.run {
            showPaymentConfirmationLiveData.observeForever(showPaymentConfirmationObserver)
            decodeJwtToken("token")
        }
        showPaymentConfirmationCaptor.run {
            verify(showPaymentConfirmationObserver).onChanged(capture())
        }
    }

    @Test
    fun `decode token error`() {
        val error = Throwable()
        whenever(serviceManager.decodePaymentRequestToken(any())).thenReturn(Single.error(error))
        viewModel.run {
            showPaymentConfirmationLiveData.observeForever(showPaymentConfirmationObserver)
            decodeJwtToken("token")
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `token is null test error`() {
        val error = Throwable()
        whenever(serviceManager.decodePaymentRequestToken(any())).thenReturn(Single.error(error))
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            decodeJwtToken(null)
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `connect to services success`() {
        whenever(serviceManager.saveService(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            newServiceLiveData.observeForever(newServiceObserver)
            payment = Payment("1", serviceName = "test")
            connectToService()
        }
        newServiceCaptor.run {
            verify(newServiceObserver).onChanged(capture())
        }
    }

    @Test
    fun `connect to services error`() {
        val error = Throwable()
        whenever(serviceManager.saveService(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.apply {
            newServiceLiveData.observeForever(newServiceObserver)
            payment = Payment("1", serviceName = "test")
            connectToService()
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `confirm transaction success test`() {
        viewModel.payment = Payment(shortName = "short")
        whenever(serviceManager.createJwtToken(any(), anyOrNull())) doReturn Single.just("token")
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.complete()
        viewModel.run {
            confirmPaymentLiveData.observeForever(confirmPaymentObserver)
            confirmTransaction()
        }
        confirmPaymentCaptor.run {
            verify(confirmPaymentObserver).onChanged(capture())
        }
    }

    @Test
    fun `confirm transaction error test`() {
        val error = Throwable()
        viewModel.payment = Payment(shortName = "short")
        whenever(serviceManager.createJwtToken(any(), anyOrNull())) doReturn Single.error(error)
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.complete()
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            confirmTransaction()
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }
}