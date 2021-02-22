package minerva.android.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.junit.Test

class ChooseIdentityViewModelTest : BaseViewModelTest() {

    private val serviceManager: ServiceManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = ChooseIdentityViewModel(serviceManager, walletActionsRepository)

    private val loginObserver: Observer<Event<LoginPayload>> = mock()
    private val loginCaptor: KArgumentCaptor<Event<LoginPayload>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val requestFieldObserver: Observer<Event<Any>> = mock()
    private val requestFieldCaptor: KArgumentCaptor<Event<Any>> = argumentCaptor()

    @Test
    fun `login to 3rd party service test success`() {
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(serviceManager.createJwtToken(any(), any())).thenReturn(Single.just("token"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.loginLiveData.observeForever(loginObserver)
        viewModel.handleLogin(
            Identity(
                1,
                personalData = linkedMapOf("name" to "Witek", "phone_number" to "123"),
                privateKey = "1",
                publicKey = "2"
            ),
            ServiceQrCode("Minerva", "callback")
        )
        loginCaptor.run {
            verify(loginObserver).onChanged(capture())
            firstValue.peekContent().loginStatus == 1
        }
    }

    @Test
    fun `login to 3rd party service invalid identity test`() {
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(serviceManager.createJwtToken(any(), any())).thenReturn(Single.just("token"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.requestedFieldsLiveData.observeForever(requestFieldObserver)
        viewModel.handleLogin(
            Identity(1, privateKey = "1", publicKey = "2"),
            ServiceQrCode("Minerva", "callback", requestedData = listOf("name"))
        )
        requestFieldCaptor.run {
            verify(requestFieldObserver).onChanged(capture())
        }
    }

    @Test
    fun `login to 3rd party service error`() {
        val error = Throwable()
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())).thenReturn(Completable.error(error))
        whenever(serviceManager.createJwtToken(any(), any())).thenReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.handleLogin(
            Identity(
                1,
                personalData = linkedMapOf("name" to "Witek", "phone_number" to "123"),
                privateKey = "1",
                publicKey = "2"
            ),
            ServiceQrCode("Minerva", "callback")
        )
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `login to 3rd party service invalid with no keys test`() {
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(serviceManager.createJwtToken(any(), any())).thenReturn(Single.just("token"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.requestedFieldsLiveData.observeForever(requestFieldObserver)
        viewModel.handleLogin(
            Identity(1, personalData = linkedMapOf("name" to "Witek", "phone_number" to "123"), privateKey = "", publicKey = ""),
            ServiceQrCode("Minerva", "callback")
        )
        requestFieldCaptor.run {
            verify(requestFieldObserver).onChanged(capture())
        }
    }
}