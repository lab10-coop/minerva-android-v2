package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils
import timber.log.Timber

class ValuesViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
    BaseViewModel() {

    private var valueName: String = String.Empty

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    private val _assetBalanceLiveData = MutableLiveData<Map<String, List<Asset>>>()
    val assetBalanceLiveData: LiveData<Map<String, List<Asset>>> get() = _assetBalanceLiveData

    private val _removeValueLiveData = MutableLiveData<Event<Unit>>()
    val removeValueLiveData: LiveData<Event<Unit>> get() = _removeValueLiveData

    fun refreshBalances() =
        launchDisposable {
            walletManager.refreshBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        _balanceLiveData.value = it
                    },
                    onError = {
                        _errorLiveData.value = Event(it)
                        Timber.d("Refresh balance error: ${it.message}")
                    }
                )
        }

    fun getAssetBalance() =
        launchDisposable {
            walletManager.refreshAssetBalance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _assetBalanceLiveData.value = it },
                    onError = {
                        _errorLiveData.value = Event(it)
                        Timber.e("Refresh asset balance error: ${it.message}")
                    }
                )
        }

    fun removeValue(index: Int, name: String) {
        valueName = name
        launchDisposable {
            walletManager.removeValue(index)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _removeValueLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Removing value with index $index failure")
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }

    fun saveRemoveValueWalletAction() {
        launchDisposable {
            walletActionsRepository.saveWalletActions(getWalletAction(), walletManager.masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { /*Handled in wallet manager */ },
                    onError = {
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }

    private fun getWalletAction() =
        WalletAction(
            WalletActionType.VALUE,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.VALUE_NAME, valueName))
        )
}