package minerva.android.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils
import minerva.android.services.login.uitls.LoginUtils.getLoginStatus
import minerva.android.services.login.uitls.LoginUtils.getRequestedData
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getServiceName
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MainViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    lateinit var loginPayload: LoginPayload

    private val _notExistedIdentityLiveData = MutableLiveData<Event<Unit>>()
    val notExistedIdentityLiveData: LiveData<Event<Unit>> get() = _notExistedIdentityLiveData

    private val _requestedFieldsLiveData = MutableLiveData<Event<String>>()
    val requestedFieldsLiveData: LiveData<Event<String>> get() = _requestedFieldsLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Pair<Int, Boolean>>>()
    val loadingLiveData: LiveData<Event<Pair<Int, Boolean>>> get() = _loadingLiveData

    fun isMasterSeedAvailable() = masterSeedRepository.isMasterSeedAvailable()

    fun initWalletConfig() = masterSeedRepository.initWalletConfig()

    fun isMnemonicRemembered(): Boolean = masterSeedRepository.isMnemonicRemembered()

    fun getValueIterator(): Int = masterSeedRepository.getValueIterator()

    fun dispose() = masterSeedRepository.dispose()

    fun loginFromNotification(jwtToken: String?) {
        jwtToken?.let {
            launchDisposable {
                serviceManager.decodeQrCodeResponse(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { handleQrCodeResponse(it) },
                        onError = { _errorLiveData.value = Event(it) }
                    )
            }
        }
    }

    private fun handleQrCodeResponse(response: QrCodeResponse) {
        response.run {
            serviceName = getServiceName(response)
            identityFields = getRequestedData(requestedData)
        }
        loginPayload = LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.issuer), response)
        painlessLogin()
    }

    fun painlessLogin() {
        serviceManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.let { identity ->
            performLogin(identity)
        }.orElse { _notExistedIdentityLiveData.value = Event(Unit) }
    }

    private fun performLogin(identity: Identity) =
        if (LoginUtils.isIdentityValid(identity)) loginPayload.qrCode?.let { minervaLogin(identity, it) }
        else _requestedFieldsLiveData.value = Event(identity.name)

    private fun minervaLogin(identity: Identity, qrCode: QrCodeResponse) {
        qrCode.callback?.let { callback ->
            launchDisposable {
                serviceManager.createJwtToken(LoginUtils.createLoginPayload(identity, qrCode))
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
}