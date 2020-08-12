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
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
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

    private val scannerResultObserver: Observer<Event<ServiceQrCode>> = mock()
    private val scannerResultCaptor: KArgumentCaptor<Event<ServiceQrCode>> = argumentCaptor()

    private val handleQrCodeResponseResultObserver: Observer<Event<String>> = mock()
    private val handleQrCodeResponseResultCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val handleQrCodeResponseErrorObserver: Observer<Event<Throwable>> = mock()
    private val handleQrCodeResponseErrorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val updateCredentialObserver: Observer<Event<CredentialQrCode>> = mock()
    private val updateCredentialCaptor: KArgumentCaptor<Event<CredentialQrCode>> = argumentCaptor()

    @Test
    fun `test validate service qr code result success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(ServiceQrCode("Minerva App")))
        viewModel.handleServiceQrCodeLiveData.observeForever(scannerResultObserver)
        viewModel.validateResult("token")
        scannerResultCaptor.run {
            verify(scannerResultObserver).onChanged(capture())
            firstValue.peekContent().serviceName == "MinervaApp"
        }
    }

    @Test
    fun `test bind credential qr code to identity success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(CredentialQrCode("Minerva App", loggedInDid = "did")))
        whenever(identityManager.bindCredentialToIdentity(any())).thenReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(identityManager.isCredentialBinded(any(), any())).doReturn(false)
        viewModel.validateResult("token")
        viewModel.bindCredentialSuccessLiveData.observeForever(handleQrCodeResponseResultObserver)
        handleQrCodeResponseResultCaptor.run {
            verify(handleQrCodeResponseResultObserver).onChanged(capture())
            firstValue.peekContent() == "name"
        }
    }

    @Test
    fun `test bind credential qr code to identity error`() {
        val error = Throwable()
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(CredentialQrCode("Minerva App", loggedInDid = "did")))
        whenever(identityManager.bindCredentialToIdentity(any())).thenReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.validateResult("token")
        viewModel.bindCredentialErrorLiveData.observeForever(handleQrCodeResponseErrorObserver)
        handleQrCodeResponseErrorCaptor.run {
            verify(handleQrCodeResponseErrorObserver).onChanged(capture())
        }
    }

    @Test
    fun `test update credential qr code success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(CredentialQrCode("Minerva App", loggedInDid = "did")))
        whenever(identityManager.isCredentialBinded(any(), any())).doReturn(true)
        viewModel.validateResult("token")
        viewModel.updateBindedCredential.observeForever(updateCredentialObserver)
        updateCredentialCaptor.run {
            verify(updateCredentialObserver).onChanged(capture())
        }
    }

    @Test
    fun `validate qr code result failed`() {
        val error = Throwable()
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.error(error))
        viewModel.handleServiceQrCodeLiveData.observeForever(scannerResultObserver)
        viewModel.validateResult("token")
        viewModel.scannerErrorLiveData.observeLiveDataEvent(Event(error))
    }
}