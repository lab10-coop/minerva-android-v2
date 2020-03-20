package minerva.android.main

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils
import minerva.android.services.login.uitls.LoginUtils.getService
import minerva.android.services.login.uitls.LoginUtils.getValuesWalletAction
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.QrCodeResponse
import timber.log.Timber

class MainViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) : BaseViewModel() {

    lateinit var loginPayload: LoginPayload

    private val _notExistedIdentityMutableLiveData = MutableLiveData<Event<Unit>>()
    val notExistedIdentityLiveData: LiveData<Event<Unit>> get() = _notExistedIdentityMutableLiveData

    private val _requestedFieldsMutableLiveData = MutableLiveData<Event<String>>()
    val requestedFieldsLiveData: LiveData<Event<String>> get() = _requestedFieldsMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    fun isMasterKeyAvailable() = walletManager.isMasterKeyAvailable()

    fun initWalletConfig() = walletManager.initWalletConfig()

    fun isMnemonicRemembered(): Boolean = walletManager.isMnemonicRemembered()

    fun getValueIterator(): Int = walletManager.getValueIterator()

    fun dispose() = walletManager.dispose()

    fun painlessLogin() {
        walletManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.let { identity ->
            performLogin(identity)
        }.orElse { _notExistedIdentityMutableLiveData.value = Event(Unit) }

    }

    private fun performLogin(identity: Identity) =
        if (LoginUtils.isIdentityValid(identity)) loginPayload.qrCode?.let { minervaLogin(identity, it) }
        else _requestedFieldsMutableLiveData.value = Event(identity.name)

    private fun minervaLogin(identity: Identity, qrCodeResponse: QrCodeResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            val jwtToken = walletManager.createJwtToken(LoginUtils.createLoginPayload(identity, qrCodeResponse), identity.privateKey)
            withContext(Dispatchers.Main) { handleLogin(qrCodeResponse, jwtToken, identity) }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun handleLogin(qrCodeResponse: QrCodeResponse, jwtToken: String, identity: Identity) {
        qrCodeResponse.callback?.let { callback ->
            walletManager.painlessLogin(callback, jwtToken, identity, getService(qrCodeResponse, identity))
                .observeOn(Schedulers.io())
                .andThen(
                    walletActionsRepository.saveWalletActions(
                        getValuesWalletAction(identity.name, qrCodeResponse.serviceName),
                        walletManager.masterKey
                    )
                )
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

    fun getIdentityName(): String? = walletManager.getLoggedInIdentity(loginPayload.identityPublicKey)?.name
}