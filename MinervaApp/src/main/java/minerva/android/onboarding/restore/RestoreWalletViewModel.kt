package minerva.android.onboarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import timber.log.Timber
import java.util.*

class RestoreWalletViewModel(private val masterSeedRepository: MasterSeedRepository) : BaseViewModel() {

    val restoreWalletLiveData: LiveData<WalletConfig> get() = masterSeedRepository.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _invalidMnemonicLiveData = MutableLiveData<Event<List<String>>>()
    val invalidMnemonicLiveData: LiveData<Event<List<String>>> get() = _invalidMnemonicLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _walletConfigNotFoundLiveData = MutableLiveData<Event<Unit>>()
    val walletConfigNotFoundLiveData: LiveData<Event<Unit>> get() = _walletConfigNotFoundLiveData

    internal fun isMnemonicLengthValid(content: CharSequence?) =
        StringTokenizer(content.toString(), String.Space).countTokens() == WORDS_IN_MNEMONIC

    internal fun validateMnemonic(mnemonic: String) {
        _loadingLiveData.value = Event(true)
        val invalidWords = masterSeedRepository.validateMnemonic(mnemonic)
        if (invalidWords.isEmpty()) {
            restoreWallet(mnemonic)
        } else {
            _loadingLiveData.value = Event(false)
            _invalidMnemonicLiveData.value = Event(invalidWords)
        }
    }

    private fun restoreWallet(mnemonic: String) {
        launchDisposable {
            masterSeedRepository.restoreMasterSeed(mnemonic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { masterSeedRepository.initWalletConfig() },
                    onError = {
                        Timber.e(it)
                        _walletConfigNotFoundLiveData.value = Event(Unit)
                    }
                )
        }
    }

    companion object {
        const val WORDS_IN_MNEMONIC = 12
    }
}