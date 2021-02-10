package minerva.android.services.login.identity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER
import minerva.android.services.login.uitls.LoginUtils.createLoginPayload
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.services.login.uitls.LoginUtils.isIdentityValid
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ChooseIdentityViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    private val _loginLiveData = MutableLiveData<Event<LoginPayload>>()
    val loginLiveData: LiveData<Event<LoginPayload>> get() = _loginLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _requestedFieldsLiveData = MutableLiveData<Event<Any>>()
    val requestedFieldsLiveData: LiveData<Event<Any>> get() = _requestedFieldsLiveData

    fun getIdentities() = serviceManager.walletConfigLiveData.value?.identities

    fun handleLogin(identity: Identity, serviceQrCode: ServiceQrCode) {
        _loadingLiveData.value = Event(true)
        if (isIdentityValid(identity, serviceQrCode.requestedData)) {
            minervaLogin(identity, serviceQrCode)
        } else {
            _loadingLiveData.value = Event(false)
            _requestedFieldsLiveData.value = Event(Any())
        }
    }

    fun handleLogin(identityIndex: Int, serviceQrCode: ServiceQrCode) {
        handleLogin(getIdentity(identityIndex), serviceQrCode)
    }

    fun getIdentityPosition(identityIndex: Int): Int =
        getIdentities()?.indexOfFirst { it.index == identityIndex } ?: Int.InvalidIndex

    private fun getIdentity(identityIndex: Int): Identity =
        getIdentities()?.find { it.index == identityIndex } ?: Identity(index = Int.InvalidIndex)

    private fun minervaLogin(identity: Identity, qrCode: ServiceQrCode) {
        qrCode.callback?.let { callback ->
            serviceManager.createJwtToken(createLoginPayload(identity, qrCode), identity.privateKey)
                .flatMapCompletable { jwtToken ->
                    serviceManager.painlessLogin(
                        callback,
                        jwtToken,
                        identity,
                        getService(qrCode, identity)
                    )
                }
                .observeOn(Schedulers.io())
                .andThen(
                    walletActionsRepository.saveWalletActions(
                        listOf(
                            getValuesWalletAction(
                                identity.name,
                                qrCode.serviceName
                            )
                        )
                    )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _loginLiveData.value = Event(LoginPayload(loginStatus = getLoginStatus(qrCode))) },
                    onError = {
                        Timber.e("Error while login $it")
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }

    private fun getLoginStatus(serviceQrCode: ServiceQrCode): Int =
        if (serviceQrCode.requestedData.contains(FCM_ID)) NEW_QUICK_USER
        else NEW_USER

    companion object {
        const val FCM_ID = "fcm_id"
        const val PAYLOAD_KEYWORD = "own"
    }
}