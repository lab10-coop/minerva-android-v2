package minerva.android.onboarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.ResponseState
import minerva.android.walletmanager.model.RestoreWalletResponse
import java.util.*

class RestoreWalletViewModel(private val walletManager: WalletManager) : ViewModel() {

    private var disposable: Disposable? = null

    private val _restoreWalletMutableLiveData = MutableLiveData<Event<RestoreWalletResponse>>()
    val restoreWalletLiveData: LiveData<Event<RestoreWalletResponse>> get() = _restoreWalletMutableLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _invalidMnemonicLiveData = MutableLiveData<Event<List<String>>>()
    val invalidMnemonicLiveData: LiveData<Event<List<String>>> get() = _invalidMnemonicLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _walletConfigNotFoundLiveData = MutableLiveData<Event<Unit>>()
    val walletConfigNotFoundLiveData: LiveData<Event<Unit>> get() = _walletConfigNotFoundLiveData

    private val _mnemonicErrorLiveData = MutableLiveData<Event<String>>()
    val mnemonicErrorLiveData: LiveData<Event<String>> get() = _mnemonicErrorLiveData

    internal fun isMnemonicLengthValid(content: CharSequence?) =
        StringTokenizer(content.toString(), String.Space).countTokens() == WORDS_IN_MNEMONIC

    internal fun validateMnemonic(mnemonic: String) {
        _loadingLiveData.value = Event(true)
        val invalidWords = walletManager.validateMnemonic(mnemonic)
        if (invalidWords.isEmpty()) {
            restoreWallet(mnemonic)
        } else {
            _loadingLiveData.value = Event(false)
            _invalidMnemonicLiveData.value = Event(invalidWords)
        }
    }

    private fun restoreWallet(mnemonic: String) {
        walletManager.restoreMasterKey(mnemonic) { error, privateKey, publicKey ->
            if (error == null) {
                disposable = walletManager.getWalletConfig(MasterKey(publicKey, privateKey))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                    .subscribeBy(
                        onSuccess = {
                            handleGetWalletConfigResponse(it)
                        },
                        onError = {
                            _errorLiveData.value = Event(it)
//                            _restoreWalletMutableLiveData.value = Event(it) uncomment when offline app is needed, test that
                        }
                    )
            } else {
                _loadingLiveData.value = Event(false)
                _mnemonicErrorLiveData.value = Event(error.localizedMessage)
            }
        }
    }

    private fun handleGetWalletConfigResponse(it: RestoreWalletResponse) {
        if (it.state == ResponseState.ERROR) {
            _walletConfigNotFoundLiveData.value = Event(Unit)
        } else {
            _restoreWalletMutableLiveData.value = Event(it)
        }
    }

    fun onPause() {
        disposable?.dispose()
    }

    companion object {
        const val WORDS_IN_MNEMONIC = 12
    }
}