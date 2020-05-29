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
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MainViewModel(private val walletManager: WalletManager, private val repository: WalletActionsRepository) : BaseViewModel() {

    lateinit var loginPayload: LoginPayload

    private val _notExistedIdentityMutableLiveData = MutableLiveData<Event<Unit>>()
    val notExistedIdentityLiveData: LiveData<Event<Unit>> get() = _notExistedIdentityMutableLiveData

    private val _requestedFieldsMutableLiveData = MutableLiveData<Event<String>>()
    val requestedFieldsLiveData: LiveData<Event<String>> get() = _requestedFieldsMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    fun isMasterSeedAvailable() = walletManager.isMasterSeedAvailable()

    fun initWalletConfig() = walletManager.initWalletConfig()

    fun isMnemonicRemembered(): Boolean = walletManager.isMnemonicRemembered()

    fun getValueIterator(): Int = walletManager.getValueIterator()

    fun dispose() = walletManager.dispose()

    fun loginFromNotification(jwtToken: String?) {
        jwtToken?.let {
            launchDisposable {
                walletManager.decodeQrCodeResponse(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { handleQrCodeResponse(it) },
                        onError = { _errorMutableLiveData.value = Event(it) }
                    )
            }
        }
    }

    private fun handleQrCodeResponse(response: QrCodeResponse) {
        response.run {
            serviceName = getServiceName(response)
            identityFields = getRequestedData(requestedData)
        }
        loginPayload = LoginPayload(getLoginStatus(response), walletManager.getLoggedInIdentityPublicKey(response.issuer), response)
        painlessLogin()
    }

    fun painlessLogin() {
        walletManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.let { identity ->
            performLogin(identity)
        }.orElse { _notExistedIdentityMutableLiveData.value = Event(Unit) }
    }

    private fun performLogin(identity: Identity) =
        if (LoginUtils.isIdentityValid(identity)) loginPayload.qrCode?.let { minervaLogin(identity, it) }
        else _requestedFieldsMutableLiveData.value = Event(identity.name)

    private fun minervaLogin(identity: Identity, qrCode: QrCodeResponse) {
        qrCode.callback?.let { callback ->
            launchDisposable {
                walletManager.createJwtToken(LoginUtils.createLoginPayload(identity, qrCode), identity.privateKey)
                    .flatMapCompletable { jwtToken ->
                        walletManager.painlessLogin(callback, jwtToken, identity, getService(qrCode, identity))
                    }
                    .observeOn(Schedulers.io())
                    .andThen(repository.saveWalletActions(getValuesWalletAction(identity.name, qrCode.serviceName), walletManager.masterSeed))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = {
                            Timber.e("Error while login $it")
                            _errorMutableLiveData.value = Event(Throwable(it.message))
                        }
                    )
            }
        }
    }

    fun getIdentityName(): String? = walletManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.name
}