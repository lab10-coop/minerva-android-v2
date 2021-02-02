package minerva.android.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.R
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.UPDATED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MainViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val orderManager: OrderManager,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    lateinit var loginPayload: LoginPayload
    lateinit var qrCode: CredentialQrCode

    private val _notExistedIdentityLiveData = MutableLiveData<Event<Unit>>()
    val notExistedIdentityLiveData: LiveData<Event<Unit>> get() = _notExistedIdentityLiveData

    private val _requestedFieldsLiveData = MutableLiveData<Event<String>>()
    val requestedFieldsLiveData: LiveData<Event<String>> get() = _requestedFieldsLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Pair<Int, Boolean>>>()
    val loadingLiveData: LiveData<Event<Pair<Int, Boolean>>> get() = _loadingLiveData

    private val _updateCredentialSuccessLiveData = MutableLiveData<Event<String>>()
    val updateCredentialSuccessLiveData: LiveData<Event<String>> get() = _updateCredentialSuccessLiveData

    private val _updateCredentialErrorLiveData = MutableLiveData<Event<Throwable>>()
    val updateCredentialErrorLiveData: LiveData<Event<Throwable>> get() = _updateCredentialErrorLiveData

    private val _updatePendingAccountLiveData = MutableLiveData<Event<PendingAccount>>()
    val updatePendingAccountLiveData: LiveData<Event<PendingAccount>> get() = _updatePendingAccountLiveData

    private val _updatePendingTransactionErrorLiveData = MutableLiveData<Event<Throwable>>()
    val updatePendingTransactionErrorLiveData: LiveData<Event<Throwable>> get() = _updatePendingTransactionErrorLiveData

    private val _handleTimeoutOnPendingTransactionsLiveData = MutableLiveData<Event<List<PendingAccount>>>()
    val handleTimeoutOnPendingTransactionsLiveData: LiveData<Event<List<PendingAccount>>> get() = _handleTimeoutOnPendingTransactionsLiveData

    val executedAccounts = mutableListOf<PendingAccount>()
    private var webSocketSubscriptions = CompositeDisposable()

    fun isMnemonicRemembered(): Boolean = masterSeedRepository.isMnemonicRemembered()
    fun getValueIterator(): Int = masterSeedRepository.getValueIterator()
    fun dispose() = masterSeedRepository.dispose()

    fun subscribeToExecutedTransactions(accountIndex: Int) {
        if (transactionRepository.shouldOpenNewWssConnection(accountIndex)) {
            webSocketSubscriptions.add(transactionRepository.subscribeToExecutedTransactions(accountIndex)
                .subscribeOn(Schedulers.io())
                .doOnComplete { restorePendingTransactions() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { handleExecutedAccounts(it) },
                    onError = {
                        Timber.e("WebSocket subscription error: $it")
                        _updatePendingTransactionErrorLiveData.value = Event(it)
                    }
                )
            )
        }
    }

    private fun handleExecutedAccounts(it: PendingAccount) {
        if (_updatePendingAccountLiveData.hasActiveObservers()) {
            transactionRepository.removePendingAccount(it)
            _updatePendingAccountLiveData.value = Event(it)
        } else executedAccounts.add(it)
    }

    fun clearWebSocketSubscription() {
        if (transactionRepository.getPendingAccounts().isEmpty()) {
            webSocketSubscriptions.clear()
        }
    }

    fun restorePendingTransactions() {
        launchDisposable {
            transactionRepository.getTransactions()
                .doOnSuccess { clearPendingAccounts() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _handleTimeoutOnPendingTransactionsLiveData.value = Event(it) },
                    onError = {
                        Timber.e("Pending transactions timeout error: $it")
                        _updatePendingTransactionErrorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun clearPendingAccounts() {
        transactionRepository.clearPendingAccounts()
    }
//todo remove when Charging Station Dashboard is not needed anymore, or prepare this service for integration
//    fun loginFromNotification(jwtToken: String?) {
//        jwtToken?.let {
//            launchDisposable {
//                serviceManager.decodeJwtToken(it)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribeBy(
//                        onSuccess = { handleQrCodeResponse(it) },
//                        onError = {
//                            Timber.e(it)
//                            _errorLiveData.value = Event(it)
//                        }
//                    )
//            }
//        }
//    }

//    private fun handleQrCodeResponse(response: QrCode) {
//        (response as? ServiceQrCode)?.apply {
//            loginPayload =
//                LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.), response)
//            painlessLogin()
//        }
//    }

    fun isOrderEditAvailable(type: Int) = orderManager.isOrderAvailable(type)

    fun painlessLogin() {
        serviceManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.let { identity ->
            loginPayload.qrCode?.requestedData?.let { requestedData ->
                performLogin(identity, requestedData)
                return
            }
        }
        _notExistedIdentityLiveData.value = Event(Unit)
    }

    private fun performLogin(identity: Identity, requestedData: List<String>) =
        if (LoginUtils.isIdentityValid(identity, requestedData)) loginPayload.qrCode?.let { minervaLogin(identity, it) }
        else _requestedFieldsLiveData.value = Event(identity.name)

    private fun minervaLogin(identity: Identity, qrCode: ServiceQrCode) {
        qrCode.callback?.let { callback ->
            launchDisposable {
                serviceManager.createJwtToken(LoginUtils.createLoginPayload(identity, qrCode), identity.privateKey)
                    .flatMapCompletable { jwtToken ->
                        serviceManager.painlessLogin(callback, jwtToken, identity, getService(qrCode, identity))
                    }
                    .observeOn(Schedulers.io())
                    .andThen(
                        walletActionsRepository.saveWalletActions(
                            listOf(getValuesWalletAction(identity.name, qrCode.serviceName))
                        )
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = {
                            Timber.e("Error while login $it")
                            _errorLiveData.value = Event(Throwable(it.message))
                        }
                    )
            }
        }
    }

    fun getIdentityName(): String? = serviceManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.name

    fun updateBindedCredentials(replace: Boolean) {
        launchDisposable {
            serviceManager.updateBindedCredential(qrCode, replace)
                .onErrorResumeNext { error ->
                    SingleSource {
                        saveWalletAction(
                            getWalletAction(
                                qrCode.lastUsed,
                                qrCode.name,
                                FAILED
                            ), error
                        )
                    }
                }
                .doOnSuccess { saveWalletAction(getWalletAction(qrCode.lastUsed, qrCode.name, UPDATED)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _updateCredentialSuccessLiveData.value = Event(it) },
                    onError = { _updateCredentialErrorLiveData.value = Event(it) }
                )
        }
    }

    fun getReplaceLabelRes(qrCode: CredentialQrCode): Int =
        if (serviceManager.isMoreCredentialToBind(qrCode)) R.string.replace_all
        else R.string.replace

    fun updateTokenIcons() {
        launchDisposable {
            transactionRepository.updateTokenIcons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { Timber.e("Checking last token icons update failed: ${it.message}") }
                )
        }

    }

    private fun saveWalletAction(walletAction: WalletAction, error: Throwable? = null) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(listOf(walletAction))
                .toSingleDefault(Pair(walletAction.status, error))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (status, error) ->
                        Timber.d("Save update binded credential wallet action success")
                        when {
                            status == FAILED && error is AutomaticBackupFailedThrowable -> _updateCredentialErrorLiveData.value =
                                Event(AutomaticBackupFailedThrowable())
                            status == FAILED -> _updateCredentialErrorLiveData.value = Event(NoBindedCredentialThrowable())

                        }
                    },
                    onError = { Timber.e("Save bind credential error: $it") }
                )
        }
    }

    private fun getWalletAction(lastUsed: Long, name: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, status,
            lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to name)
        )

    fun clearAndUnsubscribe() {
        clearPendingAccounts()
        clearWebSocketSubscription()
    }

    val isBackupAllowed: Boolean
        get() = masterSeedRepository.isBackupAllowed
}