package minerva.android.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils
import minerva.android.services.login.uitls.LoginUtils.getLoginStatus
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.UPDATED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MainViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val orderManager: OrderManager,
    private val identityManager: IdentityManager
) : BaseViewModel() {

    lateinit var loginPayload: LoginPayload
    lateinit var credential: Credential

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

    fun isMnemonicRemembered(): Boolean = masterSeedRepository.isMnemonicRemembered()

    fun getValueIterator(): Int = masterSeedRepository.getValueIterator()

    fun dispose() = masterSeedRepository.dispose()

    fun loginFromNotification(jwtToken: String?) {
        jwtToken?.let {
            launchDisposable {
                serviceManager.decodeJwtToken(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { handleQrCodeResponse(it) },
                        onError = {
                            Timber.e(it)
                            _errorLiveData.value = Event(it)
                        }
                    )
            }
        }
    }

    fun isOrderEditAvailable(type: Int) = orderManager.isOrderAvailable(type)

    private fun handleQrCodeResponse(response: QrCode) {
        (response as? ServiceQrCode)?.apply {
            loginPayload = LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.issuer), response)
            painlessLogin()
        }
    }

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
                    .andThen(walletActionsRepository.saveWalletActions(getValuesWalletAction(identity.name, qrCode.serviceName)))
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

    fun updateBindedCredential() {
        launchDisposable {
            identityManager.updateBindedCredential(credential)
                .onErrorResumeNext { SingleSource { saveWalletAction(getWalletAction(credential, FAILED)) } }
                .doOnSuccess { saveWalletAction(getWalletAction(credential, UPDATED)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _updateCredentialSuccessLiveData.value = Event(it) },
                    onError = { _updateCredentialErrorLiveData.value = Event(it) }
                )
        }
    }

    private fun saveWalletAction(walletAction: WalletAction) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(walletAction)
                .toSingleDefault(walletAction.status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        if (it == FAILED)
                            _updateCredentialErrorLiveData.value = Event(NoBindedCredentialThrowable())
                        Timber.d("Save update binded credential wallet action success")
                    },
                    onError = { Timber.e("Save bind credential error: $it") }
                )
        }
    }

    private fun getWalletAction(credential: Credential, status: Int): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, status,
            credential.lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to credential.name)
        )
}