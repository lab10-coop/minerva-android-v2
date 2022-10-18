package minerva.android.onboarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.onboarding.restore.state.*
import minerva.android.walletmanager.exception.WalletConfigNotFoundThrowable
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import timber.log.Timber
import java.util.*

class RestoreWalletViewModel(private val masterSeedRepository: MasterSeedRepository) : BaseViewModel() {
    internal var masterSeed: MasterSeed? = null
    val restoreWalletSuccess: LiveData<Event<WalletConfig>> get() = masterSeedRepository.walletConfigLiveData
    private val _restoreWalletState = MutableLiveData<RestoreWalletState>()
    val restoreWalletState: LiveData<RestoreWalletState> get() = _restoreWalletState

    fun validateMnemonic(content: CharSequence?) {
        val mnemonicAndPassword: String = content.toString()
        val mnemonicSize: Int = StringTokenizer(mnemonicAndPassword, String.Space).countTokens()
        val isMnemonicSizeValid: Boolean =
            (mnemonicSize.rem(DIVIDER) == 0 || mnemonicSize.dec().rem(DIVIDER) == 0 ) &&
                    (mnemonicSize in MIN_MNEMONIC_SIZE..MAX_MNEMONIC_SIZE)
        if (isMnemonicSizeValid) {
            checkMnemonicWords(mnemonicAndPassword)
        } else {
            _restoreWalletState.value = InvalidMnemonicLength
        }
    }

    private fun checkMnemonicWords(mnemonicAndPassword: String) {
        if (masterSeedRepository.areMnemonicWordsValid(mnemonicAndPassword)) {
            restoreSeedWithMasterKeys(mnemonicAndPassword)
        } else {
            _restoreWalletState.value = InvalidMnemonicWords
        }
    }

    private fun restoreSeedWithMasterKeys(mnemonicAndPassword: String) {
        when (val masterSeed = masterSeedRepository.restoreMasterSeed(mnemonicAndPassword)) {
            is MasterSeed -> {
                this.masterSeed = masterSeed
                _restoreWalletState.value = ValidMnemonic
            }
            else -> _restoreWalletState.value = InvalidMnemonicWords
        }
    }

    fun restoreWallet() {
        masterSeed?.let { seed ->
            launchDisposable {
                masterSeedRepository.restoreWalletConfig(seed)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { _restoreWalletState.value = Loading(true) }
                    .doOnEvent { _restoreWalletState.value = Loading(false) }
                    .subscribeBy(
                        onComplete = {
                            masterSeedRepository.apply {
                                initWalletConfig()
                                saveIsMnemonicRemembered()
                            }
                        },
                        onError = { error ->
                            _restoreWalletState.value = getErrorState(error)
                            Timber.e(error)
                        }
                    )
            }
        }
    }

    fun createWalletConfig() {
        launchDisposable {
            masterSeedRepository.createWalletConfig(masterSeed)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _restoreWalletState.value = Loading(true) }
                .doOnEvent { _restoreWalletState.value = Loading(false) }
                .doOnTerminate { _restoreWalletState.value = WalletConfigCreated }
                .subscribeBy(onError = { Timber.e("Create wallet error: $it") })
        }
    }

    private fun getErrorState(error: Throwable): RestoreWalletState =
        if (error is WalletConfigNotFoundThrowable) {
            WalletConfigNotFound
        } else {
            GenericServerError
        }

    companion object {
        private const val DIVIDER = 3
        private const val MIN_MNEMONIC_SIZE = 12
        private const val MAX_MNEMONIC_SIZE = 25
    }
}