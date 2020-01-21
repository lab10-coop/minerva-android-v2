package minerva.android.services.login.identity

import android.widget.Toast
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
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.IdentityField.Companion.PHONE_NUMBER
import minerva.android.walletmanager.model.QrCodeResponse
import timber.log.Timber

class ChooseIdentityViewModel(private val walletManager: WalletManager) : ViewModel() {

    private val _loginMutableLiveData = MutableLiveData<Event<Any>>()
    val loginLiveData: LiveData<Event<Any>> get() = _loginMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _requestedFieldsMutableLiveData = MutableLiveData<Event<Any>>()
    val requestedFieldsLiveData: LiveData<Event<Any>> get() = _requestedFieldsMutableLiveData

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
        if(identity.publicKey == String.Empty || identity.privateKey == String.Empty) {
            _errorMutableLiveData.value = Event(Throwable("Missing calculated keys"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val jwtToken = walletManager.createJwtToken(createLoginPayload(identity, identity.publicKey), identity.privateKey)
            withContext(Dispatchers.Main) {
                handleLogin(qrCodeResponse, jwtToken, identity)
            }
        }
    }

    private fun handleLogin(
        qrCodeResponse: QrCodeResponse,
        jwtToken: String,
        identity: Identity
    ) {
        qrCodeResponse.callback?.let { callback ->
            walletManager.painlessLogin(callback, jwtToken, identity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _loginMutableLiveData.value = Event(Any())
                    },
                    onError = {
                        Timber.d(it)
                        _errorMutableLiveData.value = Event(it)
                    }
                )
        }
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