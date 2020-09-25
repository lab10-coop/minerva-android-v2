package minerva.android.main

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class MainViewModelTest : BaseViewModelTest() {

    private val serviceManager: ServiceManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val masterSeedRepository: MasterSeedRepository = mock()
    private val orderManager: OrderManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val viewModel = MainViewModel(masterSeedRepository, serviceManager, walletActionsRepository, orderManager, transactionRepository)

    private val notExistedIdentityObserver: Observer<Event<Unit>> = mock()
    private val notExistedIdentityCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val requestedFieldsObserver: Observer<Event<String>> = mock()
    private val requestedFieldsCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val updateCredentialObserver: Observer<Event<String>> = mock()
    private val updateCredentialCaptor: KArgumentCaptor<Event<String>> = argumentCaptor()

    private val updatePendingAccountObserver: Observer<Event<PendingAccount>> = mock()
    private val updatePendingAccountCaptor: KArgumentCaptor<Event<PendingAccount>> = argumentCaptor()

    private val timeoutPendingAccountObserver: Observer<Event<List<PendingAccount>>> = mock()
    private val timeoutPendingAccountCaptor: KArgumentCaptor<Event<List<PendingAccount>>> = argumentCaptor()

    @Test
    fun `test known user login when there is no identity`() {
        viewModel.loginPayload = LoginPayload(1, identityPublicKey = "123")
        whenever(serviceManager.getLoggedInIdentity(any())).thenReturn(null)
        viewModel.run {
            notExistedIdentityLiveData.observeForever(notExistedIdentityObserver)
            painlessLogin()
        }
        notExistedIdentityCaptor.run {
            verify(notExistedIdentityObserver).onChanged(capture())
        }
    }

    @Test
    fun `test known user login when there is no required fields`() {
        val qrCode = ServiceQrCode(requestedData = listOf("name"))
        viewModel.loginPayload = LoginPayload(1, identityPublicKey = "123", qrCode = qrCode)
        whenever(serviceManager.getLoggedInIdentity(any())).thenReturn(Identity(1, name = "tom"))
        viewModel.run {
            requestedFieldsLiveData.observeForever(requestedFieldsObserver)
            painlessLogin()
        }
        requestedFieldsCaptor.run {
            verify(requestedFieldsObserver).onChanged(capture())
            firstValue.peekContent() == "tom"
        }
    }

    @Test
    fun `test painless login error`() {
        val error = Throwable()
        viewModel.loginPayload = LoginPayload(qrCode = ServiceQrCode(callback = "url"), loginStatus = 0)
        whenever(serviceManager.getLoggedInIdentity(any())).thenReturn(
            Identity(
                1, personalData = linkedMapOf("name" to "tom", "phone_number" to "123"),
                privateKey = "1", publicKey = "2"
            )
        )
        whenever(serviceManager.createJwtToken(any(), any())) doReturn Single.error(error)
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())) doReturn Completable.error(error)
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.error(error)
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            painlessLogin()
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `login from notification error`() {
        val error = Throwable()
        viewModel.loginPayload = LoginPayload(qrCode = ServiceQrCode(callback = "url"), loginStatus = 0)
        whenever(serviceManager.decodeJwtToken(any())) doReturn Single.just(ServiceQrCode() as QrCode)
        whenever(serviceManager.getLoggedInIdentityPublicKey(any())) doReturn "publicKey"
        whenever(serviceManager.getLoggedInIdentity(any())).thenReturn(
            Identity(
                1, personalData = linkedMapOf("name" to "tom", "phone_number" to "123"),
                privateKey = "1", publicKey = "2"
            )
        )
        whenever(serviceManager.createJwtToken(any(), any())) doReturn Single.error(error)
        whenever(serviceManager.painlessLogin(any(), any(), any(), any())) doReturn Completable.error(error)
        whenever(walletActionsRepository.saveWalletActions(any())) doReturn Completable.error(error)
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            loginFromNotification("token")
        }

        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `Should show edit order icon`() {
        whenever(orderManager.isOrderAvailable(any())).thenReturn(true)
        val isEditIconVisible = viewModel.isOrderEditAvailable(WalletActionType.IDENTITY)
        isEditIconVisible shouldBeEqualTo true
    }

    @Test
    fun `Should not show edit order icon`() {
        whenever(orderManager.isOrderAvailable(any())).thenReturn(false)
        val isEditIconVisible = viewModel.isOrderEditAvailable(WalletActionType.IDENTITY)
        isEditIconVisible shouldBeEqualTo false
    }

    @Test
    fun `update binded credential test`() {
        whenever(serviceManager.updateBindedCredential(any())).doReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).doReturn(Completable.complete())
        viewModel.run {
            qrCode = CredentialQrCode("name", "type")
            updateCredentialSuccessLiveData.observeForever(updateCredentialObserver)
            updateBindedCredential()
        }
        updateCredentialCaptor.run {
            verify(updateCredentialObserver).onChanged(capture())
            firstValue.peekContent() == "name"
        }
    }

    @Test
    fun `update binded credential test error`() {
        val error = Throwable()
        whenever(serviceManager.updateBindedCredential(any())).doReturn(Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).doReturn(Completable.complete())
        viewModel.run {
            qrCode = CredentialQrCode("name", "type")
            updateCredentialErrorLiveData.observeForever(errorObserver)
            updateBindedCredential()
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `subscribe to executed transactions success`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.subscribeToExecutedTransactions(any())).thenReturn(Flowable.just(PendingAccount(1, "123")))
        viewModel.run {
            updatePendingAccountLiveData.observeForever(updatePendingAccountObserver)
            subscribeToExecutedTransactions(1)

        }
        updatePendingAccountCaptor.run {
            verify(updatePendingAccountObserver).onChanged(capture())
            firstValue.peekContent().txHash == "123"
        }
    }

    @Test
    fun `subscribe to executed transactions error`() {
        val error = Throwable()
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.subscribeToExecutedTransactions(any())).thenReturn(Flowable.error(error))
        viewModel.run {
            updatePendingTransactionErrorLiveData.observeForever(errorObserver)
            subscribeToExecutedTransactions(1)

        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `subscribe to executed transactions on complete`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.subscribeToExecutedTransactions(any())).thenReturn(Flowable.just(PendingAccount(1, "123")).take(0))
        whenever(transactionRepository.getTransactions()).thenReturn(Single.just(listOf(PendingAccount(1, "123"))))
        viewModel.run {
            handleTimeoutOnPendingTransactionsLiveData.observeForever(timeoutPendingAccountObserver)
            subscribeToExecutedTransactions(1)

        }
        timeoutPendingAccountCaptor.run {
            verify(timeoutPendingAccountObserver).onChanged(capture())
            firstValue.peekContent()[0].txHash == "123"
        }
    }

    @Test
    fun `subscribe to executed transactions on complete and error occurs`() {
        val error = Throwable()
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.subscribeToExecutedTransactions(any())).thenReturn(Flowable.just(PendingAccount(1, "123")).take(0))
        whenever(transactionRepository.getTransactions()).thenReturn(Single.error(error))
        viewModel.run {
            updatePendingTransactionErrorLiveData.observeForever(errorObserver)
            subscribeToExecutedTransactions(1)

        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }


    @Test
    fun `restore pending transactions success`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.getTransactions()).thenReturn(Single.just(listOf(PendingAccount(1, "123"))))
        viewModel.run {
            handleTimeoutOnPendingTransactionsLiveData.observeForever(timeoutPendingAccountObserver)
            restorePendingTransactions()

        }
        timeoutPendingAccountCaptor.run {
            verify(timeoutPendingAccountObserver).onChanged(capture())
            firstValue.peekContent()[0].txHash == "123"
        }
    }

    @Test
    fun `restore pending transactions error`() {
        val error = Throwable()
        whenever(transactionRepository.getPendingAccounts()).thenReturn(listOf(PendingAccount(1, "123")))
        whenever(transactionRepository.getTransactions()).thenReturn(Single.error(error))
        viewModel.run {
            updatePendingTransactionErrorLiveData.observeForever(errorObserver)
            restorePendingTransactions()

        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }
}