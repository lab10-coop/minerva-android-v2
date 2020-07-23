package minerva.android.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.services.login.scanner.LoginScannerViewModel
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.QrCodeResponse
import org.junit.Test

class ScannerViewModelTest : BaseViewModelTest() {

    private val serviceManager: ServiceManager = mock()
    private val viewModel = LoginScannerViewModel(serviceManager)

    private val scannerResultObserver: Observer<Event<QrCodeResponse>> = mock()
    private val scannerResultCaptor: KArgumentCaptor<Event<QrCodeResponse>> = argumentCaptor()

    @Test
    fun `test validate qr code result success`() {
        whenever(serviceManager.decodeQrCodeResponse(any())).thenReturn(Single.just(QrCodeResponse("Minerva App")))
        viewModel.scannerResultLiveData.observeForever(scannerResultObserver)
        viewModel.validateResult("token")
        scannerResultCaptor.run {
            verify(scannerResultObserver).onChanged(capture())
            firstValue.peekContent().serviceName == "MinervaApp"
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