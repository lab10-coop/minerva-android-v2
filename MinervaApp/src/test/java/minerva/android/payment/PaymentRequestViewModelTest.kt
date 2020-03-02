package minerva.android.payment

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.InvalidVersion
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.Payment
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletConfig
import org.junit.Test

class PaymentRequestViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = PaymentRequestViewModel(walletManager, walletActionsRepository)

    private val showPaymentConfirmationObserver: Observer<Event<Unit>> = mock()
    private val showPaymentConfirmationCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val showConnectionRequestObserver: Observer<Event<String?>> = mock()
    private val showConnectionRequestCaptor: KArgumentCaptor<Event<String?>> = argumentCaptor()

    private val newServiceObserver: Observer<Event<Unit>> = mock()
    private val newServiceCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `decode token success when service is not connected`() {
        whenever(walletManager.decodePaymentRequestToken(any())).thenReturn(Single.just(Pair(Payment("12", serviceName = "test"), listOf())))
        viewModel.showConnectionRequestLiveData.observeForever(showConnectionRequestObserver)
        viewModel.decodeJwtToken("token")
        showConnectionRequestCaptor.run {
            verify(showConnectionRequestObserver).onChanged(capture())
            firstValue.peekContent() == "test"
        }
    }

    @Test
    fun `decode token success when service is connected`() {
        whenever(walletManager.decodePaymentRequestToken(any())).thenReturn(Single.just(Pair(Payment("12"), listOf(Service(1, name = "M27")))))
        viewModel.showPaymentConfirmationLiveData.observeForever(showPaymentConfirmationObserver)
        viewModel.decodeJwtToken("token")
        showPaymentConfirmationCaptor.run {
            verify(showPaymentConfirmationObserver).onChanged(capture())
        }
    }

    @Test
    fun `decode token error`() {
        val error = Throwable()
        whenever(walletManager.decodePaymentRequestToken(any())).thenReturn(Single.error(error))
        viewModel.showPaymentConfirmationLiveData.observeForever(showPaymentConfirmationObserver)
        viewModel.decodeJwtToken("token")
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }


    @Test
    fun `token is null test error`() {
        val error = Throwable()
        whenever(walletManager.decodePaymentRequestToken(any())).thenReturn(Single.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.decodeJwtToken(null)
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `connect to services success`() {
        whenever(walletManager.saveService(any())).thenReturn(Single.just(WalletConfig(Int.InvalidVersion)))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.newServiceMutableLiveData.observeForever(newServiceObserver)
        viewModel.payment = Payment("1", serviceName = "test")
        viewModel.connectToService()
        newServiceCaptor.run {
            verify(newServiceObserver).onChanged(capture())
        }
    }

    @Test
    fun `connect to services error`() {
        val error = Throwable()
        whenever(walletManager.saveService(any())).thenReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.apply {
            newServiceMutableLiveData.observeForever(newServiceObserver)
            payment = Payment("1", serviceName = "test")
            connectToService()
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}