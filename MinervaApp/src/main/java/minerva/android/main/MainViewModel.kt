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
import minerva.android.main.error.*
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.UPDATED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.widget.state.AppUIState
import timber.log.Timber
import java.net.ConnectException

class MainViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val orderManager: OrderManager,
    private val tokenManager: TokenManager,
    private val transactionRepository: TransactionRepository,
    private val appUIState: AppUIState
) : BaseViewModel() {
    lateinit var loginPayload: LoginPayload
    lateinit var qrCode: CredentialQrCode
    private var webSocketSubscriptions = CompositeDisposable()
    val executedAccounts = mutableListOf<PendingAccount>()
    val isBackupAllowed: Boolean get() = masterSeedRepository.isBackupAllowed
    fun isMnemonicRemembered(): Boolean = masterSeedRepository.isMnemonicRemembered()
    fun isProtectTransactionEnabled() = transactionRepository.isProtectTransactionEnabled()

    private val _errorLiveData = MutableLiveData<Event<MainErrorState>>()
    val errorLiveData: LiveData<Event<MainErrorState>> get() = _errorLiveData

    private val _handleTimeoutOnPendingTransactionsLiveData = MutableLiveData<Event<List<PendingAccount>>>()
    val handleTimeoutOnPendingTransactionsLiveData: LiveData<Event<List<PendingAccount>>> get() = _handleTimeoutOnPendingTransactionsLiveData

    private val _updateCredentialSuccessLiveData = MutableLiveData<Event<String>>()
    val updateCredentialSuccessLiveData: LiveData<Event<String>> get() = _updateCredentialSuccessLiveData

    private val _updatePendingAccountLiveData = MutableLiveData<Event<PendingAccount>>()
    val updatePendingAccountLiveData: LiveData<Event<PendingAccount>> get() = _updatePendingAccountLiveData

    private val _updateTokensRateLiveData = MutableLiveData<Event<Unit>>()
    val updateTokensRateLiveData: LiveData<Event<Unit>> get() = _updateTokensRateLiveData

    private val _redirectToSplashScreenLiveData = MutableLiveData<Event<Unit>>()
    val redirectToSplashScreenLiveData: LiveData<Event<Unit>> get() = _redirectToSplashScreenLiveData

    init {
        if (shouldShowSplashScreen()) {
            _redirectToSplashScreenLiveData.value = Event(Unit)
        }
    }

    @Throws(ConnectException::class)
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
                        _errorLiveData.value = Event(UpdatePendingTransactionError)
                    }
                )
            )
        }
    }

    fun restorePendingTransactions() {
        launchDisposable {
            transactionRepository.getTransactions()
                .doOnSuccess { clearPendingAccounts() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { if (it.isNotEmpty()) _handleTimeoutOnPendingTransactionsLiveData.value = Event(it) },
                    onError = {
                        Timber.e("Pending transactions timeout error: $it")
                        _errorLiveData.value = Event(UpdatePendingTransactionError)
                    }
                )
        }
    }

    fun isOrderEditAvailable(type: Int): Boolean =
        if (!appUIState.shouldShowSplashScreen) orderManager.isOrderAvailable(type) else false

    fun shouldShowSplashScreen() = appUIState.shouldShowSplashScreen

    fun painlessLogin() {
        serviceManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.let { identity ->
            loginPayload.qrCode?.requestedData?.let { requestedData ->
                performLogin(identity, requestedData)
                return
            }
        }
        _errorLiveData.value = Event(NotExistedIdentity)
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
                    onError = { _errorLiveData.value = Event(UpdateCredentialError()) }
                )
        }
    }

    fun getReplaceLabelRes(qrCode: CredentialQrCode): Int =
        if (serviceManager.isMoreCredentialToBind(qrCode)) R.string.replace_all
        else R.string.replace

    fun checkMissingTokensDetails() {
        launchDisposable {
            transactionRepository.checkMissingTokensDetails()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { Timber.e("Checking last token icons update failed: ${it.message}") })
        }
    }

    fun getTokensRate() {
        launchDisposable {
            transactionRepository.getTokensRates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _updateTokensRateLiveData.value = Event(Unit) },
                    onError = { Timber.e(it) }
                )
        }
    }

    fun discoverNewTokens() =
        launchDisposable {
            transactionRepository.discoverNewTokens()
                .filter { shouldRefreshTokensBalances -> shouldRefreshTokensBalances }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        fetchNFTData()
                    },
                    onError = { error -> Timber.e("Error while token auto-discovery: $error") }
                )
        }

    private fun fetchNFTData() {
        launchDisposable {
            tokenManager.fetchNFTsDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                    }
                )
        }
    }

    private fun handleExecutedAccounts(it: PendingAccount) {
        if (_updatePendingAccountLiveData.hasActiveObservers()) {
            transactionRepository.removePendingAccount(it)
            _updatePendingAccountLiveData.value = Event(it)
        } else executedAccounts.add(it)
    }

    private fun performLogin(identity: Identity, requestedData: List<String>) =
        if (LoginUtils.isIdentityValid(identity, requestedData)) loginPayload.qrCode?.let { minervaLogin(identity, it) }
        else _errorLiveData.value = Event(RequestedFields(identity.name))

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
                            _errorLiveData.value = Event(BaseError)
                        }
                    )
            }
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
                        val throwable = getUpdateCredentialsEventThrowable(status, error)
                        _errorLiveData.value = Event(UpdateCredentialError(throwable))
                    },
                    onError = { Timber.e("Save bind credential error: $it") }
                )
        }
    }

    private fun getUpdateCredentialsEventThrowable(status: Int, error: Throwable?) = when {
        status == FAILED && error is AutomaticBackupFailedThrowable -> AutomaticBackupFailedThrowable()
        status == FAILED -> NoBindedCredentialThrowable()
        else -> Throwable()
    }

    private fun getWalletAction(lastUsed: Long, name: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, status,
            lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to name)
        )

    fun dispose() {
        masterSeedRepository.dispose()
    }

    fun clearWebSocketSubscription() {
        if (transactionRepository.getPendingAccounts().isEmpty()) {
            webSocketSubscriptions.clear()
        }
    }

    fun clearAndUnsubscribe() {
        clearPendingAccounts()
        clearWebSocketSubscription()
    }

    fun clearPendingAccounts() {
        transactionRepository.clearPendingAccounts()
    }
}