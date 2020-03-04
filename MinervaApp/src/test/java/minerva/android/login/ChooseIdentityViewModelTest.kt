package minerva.android.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.QrCodeResponse
import org.amshove.kluent.shouldEqualTo
import org.junit.Test

class ChooseIdentityViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = ChooseIdentityViewModel(walletManager, walletActionsRepository)

    private val loginObserver: Observer<Event<Any>> = mock()
    private val loginCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `login to 3rd party service test success`() {
        whenever(walletManager.painlessLogin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.loginLiveData.observeForever(loginObserver)
        viewModel.handleLogin(QrCodeResponse("Minerva", "callback"), "jwtToken", Identity(1))
        loginCaptor.run {
            verify(loginObserver).onChanged(capture())
            firstValue.peekContent() == Unit
        }
    }

    @Test
    fun `login to 3rd party service test error`() {
        val error = Throwable()
        whenever(walletManager.painlessLogin(any(), any(), any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        doNothing().whenever(walletManager).initWalletConfig()
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.handleLogin(QrCodeResponse("Minerva", "callback"), "jwtToken", Identity(1))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `is identity valid test`() {
        val result1 = viewModel.handleNoKeysError(Identity(1))
        result1 shouldEqualTo true

        val result2 = viewModel.handleNoKeysError(Identity(1, publicKey = "12", privateKey = "345"))
        result2 shouldEqualTo false
    }
}