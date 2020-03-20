package minerva.android.main

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.QrCodeResponse
import org.junit.Test

class MainViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = MainViewModel(walletManager, walletActionsRepository)

    private val notExistedIdentityObserver: Observer<Event<Unit>> = mock()
    private val notExistedIdentityCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val requestedFieldsObserver: Observer<Event<String>> = mock()
    private val requestedFieldsCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `test known user login when there is no identity`() {
        viewModel.loginPayload = LoginPayload(1, identityPublicKey = "123")
        whenever(walletManager.getLoggedInIdentity(any())).thenReturn(null)
        viewModel.notExistedIdentityLiveData.observeForever(notExistedIdentityObserver)
        viewModel.painlessLogin()
        notExistedIdentityCaptor.run {
            verify(notExistedIdentityObserver).onChanged(capture())
        }
    }

    @Test
    fun `test known user login when there is no required fields`() {
        viewModel.loginPayload = LoginPayload(1, identityPublicKey = "123")
        whenever(walletManager.getLoggedInIdentity(any())).thenReturn(Identity(1, name = "witek"))
        viewModel.requestedFieldsLiveData.observeForever(requestedFieldsObserver)
        viewModel.painlessLogin()
        requestedFieldsCaptor.run {
            verify(requestedFieldsObserver).onChanged(capture())
            firstValue.peekContent() == "witek"
        }
    }

    @Test
    fun `test known user login error`() {
        val error = Throwable()
        whenever(walletManager.painlessLogin(any(), any(), any(), any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.handleLogin(QrCodeResponse("test", "callback"), "jwt", Identity(1, name = "name"))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }
}