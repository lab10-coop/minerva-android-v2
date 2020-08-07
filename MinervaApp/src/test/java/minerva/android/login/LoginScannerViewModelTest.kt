package minerva.android.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.services.login.scanner.LoginScannerViewModel
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.CredentialQrResponse
import minerva.android.walletmanager.model.ServiceQrResponse
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Before
import org.junit.Test

class LoginScannerViewModelTest : BaseViewModelTest() {

    private val serviceManager: ServiceManager = mock()
    private val identityManager: IdentityManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private lateinit var viewModel: LoginScannerViewModel

    @Before
    fun setup() {
        viewModel = LoginScannerViewModel(serviceManager, walletActionsRepository, identityManager)
    }

    private val scannerResultObserver: Observer<Event<ServiceQrResponse>> = mock()
    private val scannerResultCaptor: KArgumentCaptor<Event<ServiceQrResponse>> = argumentCaptor()

    private val handleQrCodeResponseResultObserver: Observer<Event<String>> = mock()
    private val handleQrCodeResponseResultCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val handleQrCodeResponseErrorObserver: Observer<Event<Throwable>> = mock()
    private val handleQrCodeResponseErrorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `test validate service qr code result success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(ServiceQrResponse("Minerva App")))
        viewModel.scannerResultLiveData.observeForever(scannerResultObserver)
        viewModel.validateResult("token")
        scannerResultCaptor.run {
            verify(scannerResultObserver).onChanged(capture())
            firstValue.peekContent().serviceName == "MinervaApp"
        }
    }

    @Test
    fun `test validate credential qr code result success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(CredentialQrResponse("Minerva App", loggedInDid = "did")))
        whenever(identityManager.bindCredentialToIdentity(any())).thenReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.validateResult("token")
        viewModel.handleBindCredentialSuccessLiveData.observeForever(handleQrCodeResponseResultObserver)
        handleQrCodeResponseResultCaptor.run {
            verify(handleQrCodeResponseResultObserver).onChanged(capture())
            firstValue.peekContent() == "name"
        }
    }

    @Test
    fun `test validate credential qr code result error`() {
        val error = Throwable()
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(CredentialQrResponse("Minerva App", loggedInDid = "did")))
        whenever(identityManager.bindCredentialToIdentity(any())).thenReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.validateResult("token")
        viewModel.handleBindCredentialErrorLiveData.observeForever(handleQrCodeResponseErrorObserver)
        handleQrCodeResponseErrorCaptor.run {
            verify(handleQrCodeResponseErrorObserver).onChanged(capture())
        }
    }

    @Test
    fun `validate qr code result failed`() {
        val error = Throwable()
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.error(error))
        viewModel.scannerResultLiveData.observeForever(scannerResultObserver)
        viewModel.validateResult("token")
        viewModel.scannerErrorLiveData.observeLiveDataEvent(Event(error))
    }
}