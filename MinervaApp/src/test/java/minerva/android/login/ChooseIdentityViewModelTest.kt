package minerva.android.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.observeWithPredicate
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.*
import org.amshove.kluent.shouldEqualTo
import org.junit.Test
import java.math.BigDecimal

class ChooseIdentityViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = ChooseIdentityViewModel(walletManager, walletActionsRepository)

    private val loginObserver: Observer<Event<Any>> = mock()

    private val saveWalletActionObserver: Observer<Event<Unit>> = mock()
    private val saveWalletActionCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()


    @Test
    fun `login to 3rd party service test success`() {
        whenever(walletManager.painlessLogin(any(), any(), any())).thenReturn(Completable.complete())
        viewModel.loginLiveData.observeForever(loginObserver)
        viewModel.handleLogin(QrCodeResponse("Minerva"), "jwtToken", Identity(1))
        viewModel.loginLiveData.observeWithPredicate { it.peekContent() == Unit }
    }

    @Test
    fun `login to 3rd party service test error`() {
        val error = Throwable()
        whenever(walletManager.painlessLogin(any(), any(), any())).thenReturn(Completable.error(error))
        viewModel.loginLiveData.observeForever(loginObserver)
        viewModel.handleLogin(QrCodeResponse("Minerva"), "jwtToken", Identity(1))
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `is identity valid test`() {
        val result1 = viewModel.handleNoKeysError(Identity(1))
        result1 shouldEqualTo true

        val result2 = viewModel.handleNoKeysError(Identity(1, publicKey = "12", privateKey = "345"))
        result2 shouldEqualTo false
    }

    @Test
    fun `save wallet action success`() {
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveLoginWalletAction()
            saveWalletActionCaptor.run {
                verify(saveWalletActionObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveLoginWalletAction()
            viewModel.errorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}