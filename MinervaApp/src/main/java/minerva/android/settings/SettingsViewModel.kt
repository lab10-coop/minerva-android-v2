package minerva.android.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.BuildConfig
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.mapper.StringArrayMapper
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.storage.LocalStorage
import timber.log.Timber

class SettingsViewModel(private val masterSeedRepository: MasterSeedRepository, private val localStorage: LocalStorage, private val  walletConfigManager: WalletConfigManager) :
    BaseViewModel() {

    private val _resetTokensLiveData = MutableLiveData<Event<Result<Any>>>()
    val resetTokensLiveData: LiveData<Event<Result<Any>>> = _resetTokensLiveData

    fun areMainNetworksEnabled(isChecked: Boolean) {
        masterSeedRepository.areMainNetworksEnabled = isChecked
    }

    val areMainNetsEnabled: Boolean
        get() = masterSeedRepository.areMainNetworksEnabled

    val isMnemonicRemembered
        get() = masterSeedRepository.isMnemonicRemembered()

    val isSynced
        get() = masterSeedRepository.isSynced

    val isAuthenticationEnabled
        get() = localStorage.isProtectKeysEnabled

    val isForDeveloperSectionEnabled
        get() = BuildConfig.FLAVOR == STAGING

    fun resetTokens() {
        launchDisposable {
            walletConfigManager.removeAllTokens()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _resetTokensLiveData.value = Event(Result.success(Unit)) },
                    onError = {
                        Timber.e(it)
                        _resetTokensLiveData.value = Event(Result.failure(it))
                    }
                )
        }

    }

    fun getCurrentFiat(currencies: Array<String>): String = localStorage.loadCurrentFiat().let { fiat ->
        StringArrayMapper.mapStringArray(currencies)[fiat]?.let {
            String.format(FIAT_HEADER_FORMAT, it, fiat)
        }.orElse { String.Empty }
    }

    companion object {
        private const val FIAT_HEADER_FORMAT = "%s (%s)"
        private const val STAGING = "staging"
    }
}