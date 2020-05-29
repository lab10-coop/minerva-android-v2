package minerva.android.services.login.identity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.NEW_USER
import minerva.android.services.login.uitls.LoginUtils.createLoginPayload
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.services.login.uitls.LoginUtils.isIdentityValid
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.IncognitoIdentity
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ChooseIdentityViewModel(private val walletManager: WalletManager, private val repository: WalletActionsRepository) : BaseViewModel() {

    private val _loginMutableLiveData = MutableLiveData<Event<LoginPayload>>()
    val loginLiveData: LiveData<Event<LoginPayload>> get() = _loginMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _requestedFieldsMutableLiveData = MutableLiveData<Event<Any>>()
    val requestedFieldsLiveData: LiveData<Event<Any>> get() = _requestedFieldsMutableLiveData

    fun getIdentities() = walletManager.walletConfigLiveData.value?.identities

    //    TODO implement dynamic login concerning different services
    fun handleLogin(identity: Identity, qrCodeResponse: QrCodeResponse) {
        _loadingLiveData.value = Event(true)
        if (isIdentityValid(identity)) {
            minervaLogin(identity, qrCodeResponse)
        } else {
            _loadingLiveData.value = Event(false)
            _requestedFieldsMutableLiveData.value = Event(Any())
        }
    }

    private fun minervaLogin(identity: Identity, qrCode: QrCodeResponse) {
        if (handleNoKeysError(identity)) return
        qrCode.callback?.let { callback ->
            walletManager.createJwtToken(createLoginPayload(identity, qrCode), identity.privateKey)
                .flatMapCompletable { jwtToken ->
                    walletManager.painlessLogin(callback, jwtToken, identity, getService(qrCode, identity))
                }
                .observeOn(Schedulers.io())
                .andThen(repository.saveWalletActions(getValuesWalletAction(identity.name, qrCode.serviceName), walletManager.masterSeed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _loginMutableLiveData.value = Event(LoginPayload(loginStatus = getLoginStatus(qrCode)))
                    },
                    onError = {
                        Timber.e("Error while login $it")
                        _errorMutableLiveData.value = Event(Throwable(it.message))
                    }
                )

        }
    }

    private fun handleNoKeysError(identity: Identity): Boolean {
        if (doesIdentityHaveKeys(identity)) {
            _errorMutableLiveData.value = Event(Throwable("Missing calculated keys"))
            return true
        }
        return false
    }

    private fun doesIdentityHaveKeys(identity: Identity) =
        identity != IncognitoIdentity() && (identity.publicKey == String.Empty || identity.privateKey == String.Empty)

    private fun getLoginStatus(qrCodeResponse: QrCodeResponse): Int =
        if (qrCodeResponse.requestedData.contains(FCM_ID)) NEW_QUICK_USER
        else NEW_USER

    companion object {
        const val PHONE = "phone"
        const val NAME = "name"
        const val IDENTITY_NO = "identity_no"
        const val FCM_ID = "fcm_id"
    }
}