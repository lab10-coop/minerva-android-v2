package minerva.android.services.login.identity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.IdentityField.Companion.PHONE_NUMBER
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils
import timber.log.Timber

class ChooseIdentityViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
    BaseViewModel() {

    private var identityName: String = String.Empty
    private var serviceName: String = String.Empty

    private val _loginMutableLiveData = MutableLiveData<Event<Unit>>()
    val loginLiveData: LiveData<Event<Unit>> get() = _loginMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _requestedFieldsMutableLiveData = MutableLiveData<Event<Any>>()
    val requestedFieldsLiveData: LiveData<Event<Any>> get() = _requestedFieldsMutableLiveData

    private val _saveWalletActionLiveData = MutableLiveData<Event<Unit>>()
    val saveWalletActionLiveData: LiveData<Event<Unit>> get() = _saveWalletActionLiveData

    fun getIdentities() = walletManager.walletConfigLiveData.value?.identities

    //    TODO implement dynamic login concerning different services
    fun handleLoginButton(identity: Identity, qrCodeResponse: QrCodeResponse) {
        _loadingLiveData.value = Event(true)
        if (isIdentityValid(identity)) {
            minervaLogin(identity, qrCodeResponse)
        } else {
            _loadingLiveData.value = Event(false)
            _requestedFieldsMutableLiveData.value = Event(Any())
        }
    }

    private fun minervaLogin(
        identity: Identity,
        qrCodeResponse: QrCodeResponse
    ) {
        if (handleNoKeysError(identity)) return
        viewModelScope.launch(Dispatchers.IO) {
            val jwtToken = walletManager.createJwtToken(createLoginPayload(identity, identity.publicKey), identity.privateKey)
            withContext(Dispatchers.Main) {
                handleLogin(qrCodeResponse, jwtToken, identity)
            }
        }
    }

    fun handleNoKeysError(identity: Identity): Boolean {
        if (doesIdentityHaveKeys(identity)) {
            _errorMutableLiveData.value = Event(Throwable("Missing calculated keys"))
            return true
        }
        return false
    }

    private fun doesIdentityHaveKeys(identity: Identity) =
        identity != IncognitoIdentity() && (identity.publicKey == String.Empty || identity.privateKey == String.Empty)

    fun handleLogin(
        qrCodeResponse: QrCodeResponse,
        jwtToken: String,
        identity: Identity
    ) {
        identityName = identity.name
        serviceName = qrCodeResponse.serviceName
        qrCodeResponse.callback?.let { callback ->
            walletManager.painlessLogin(callback, jwtToken, identity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _loginMutableLiveData.value = Event(Unit) },
                    onError = {
                        Timber.d(it)
                        _errorMutableLiveData.value = Event(it)
                    }
                )
        }
    }

    fun saveLoginWalletAction() {
        launchDisposable {
            walletActionsRepository.saveWalletActions(getValuesWalletAction(), walletManager.masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _saveWalletActionLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e(it.message)
                        _errorMutableLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun getValuesWalletAction(): WalletAction {
        return WalletAction(
            WalletActionType.SERVICE,
            WalletActionStatus.LOG_IN,
            DateUtils.timestamp,
            hashMapOf(
                Pair(WalletActionFields.IDENTITY_NAME, identityName),
                Pair(WalletActionFields.SERVICE, serviceName)
            )
        )
    }

    //todo change it to dynamic requested fields creation
    private fun isIdentityValid(identity: Identity) =
        identity.data[PHONE_NUMBER] != null && identity.data[NAME] != null

    //todo change it to dynamic payload creation
    private fun createLoginPayload(
        identity: Identity,
        publicKey: String
    ): Map<String, String?> {
        return mapOf(
            Pair(PHONE, identity.data[PHONE_NUMBER]),
            Pair(NAME, identity.data[NAME]),
            Pair(IDENTITY_NO, publicKey)
        )
    }

    companion object {
        const val PHONE = "phone"
        const val NAME = "name"
        const val IDENTITY_NO = "identity_no"
    }
}