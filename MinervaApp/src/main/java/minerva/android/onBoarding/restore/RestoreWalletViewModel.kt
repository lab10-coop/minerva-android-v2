package minerva.android.onBoarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Space
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Identity
import timber.log.Timber
import java.util.*

class RestoreWalletViewModel(private val walletManager: WalletManager) : ViewModel() {

    private val validateMnemonicMutableLiveData = MutableLiveData<List<String>>()
    val validateMnemonicLiveData: LiveData<List<String>> get() = validateMnemonicMutableLiveData

    internal fun isMnemonicLengthValid(content: CharSequence?) =
        StringTokenizer(content.toString(), String.Space).countTokens() == WORDS_IN_MNEMONIC

    internal fun validateMnemonic(mnemonic: String) {
        validateMnemonicMutableLiveData.value = walletManager.validateMnemonic(mnemonic)
    }

    companion object {
        const val WORDS_IN_MNEMONIC = 12
    }
}