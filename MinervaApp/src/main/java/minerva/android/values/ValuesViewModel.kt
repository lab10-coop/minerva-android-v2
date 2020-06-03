package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils
import timber.log.Timber
import java.math.BigDecimal

class ValuesViewModel(
    private val walletManager: WalletManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractManager: SmartContractManager
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData = MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyAndHasMoreOwnersErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData

    private val _isNotSafeAccountMasterOwnerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val isNotSafeAccountMasterOwnerErrorLiveData: LiveData<Event<Throwable>> get() = _isNotSafeAccountMasterOwnerErrorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    private val _assetBalanceLiveData = MutableLiveData<Map<String, List<Asset>>>()
    val assetBalanceLiveData: LiveData<Map<String, List<Asset>>> get() = _assetBalanceLiveData

    private val _noFundsLiveData = MutableLiveData<Event<Unit>>()
    val noFundsLiveData: LiveData<Event<Unit>> get() = _noFundsLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun refreshBalances() =
        launchDisposable {
            walletManager.refreshBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _balanceLiveData.value = it },
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

    fun removeValue(value: Value) {
        launchDisposable {
            walletManager.removeValue(value.index)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(getRemovedValueAction(value), walletManager.masterSeed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = {
                        Timber.e("Removing value with index ${value.index} failure")
                        when (it) {
                            is BalanceIsNotEmptyAndHasMoreOwnersThrowable -> _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.value = Event(it)
                            is IsNotSafeAccountMasterOwnerThrowable -> _isNotSafeAccountMasterOwnerErrorLiveData.value = Event(it)
                            else -> _errorLiveData.value = Event(Throwable(it.message))
                        }
                    }
                )
        }
    }

    private fun getRemovedValueAction(value: Value) =
        value.run {
            if (isSafeAccount) getWalletAction(SAFE_ACCOUNT_REMOVED, name)
            else getWalletAction(REMOVED, name)
        }

    fun createSafeAccount(value: Value) {
        if (value.cryptoBalance == BigDecimal.ZERO) {
            _noFundsLiveData.value = Event(Unit)
        } else {
            launchDisposable {
                smartContractManager.createSafeAccount(value)
                    .flatMapCompletable { smartContractAddress -> createValue(value, smartContractAddress) }
                    .observeOn(Schedulers.io())
                    .andThen(
                        walletActionsRepository.saveWalletActions(
                            getWalletAction(SAFE_ACCOUNT_ADDED, createSafeAccountName(value)), walletManager.masterSeed
                        )
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { _loadingLiveData.value = Event(true) }
                    .doOnEvent { _loadingLiveData.value = Event(false) }
                    .subscribeBy(
                        onComplete = { /*Handled in wallet manager */ },
                        onError = {
                            Timber.e("Creating safe account error: ${it.message}")
                            _errorLiveData.value = Event(Throwable(it.message))
                        }
                    )
            }
        }
    }

    private fun createValue(value: Value, smartContractAddress: String): Completable {
        return walletManager.createValue(
            Network.fromString(value.network),
            createSafeAccountName(value),
            value.address,
            smartContractAddress
        )
    }

    private fun createSafeAccountName(value: Value): String =
        value.name.replaceFirst(String.Space, " | ${walletManager.getSafeAccountNumber(value.address)} ")

    private fun getWalletAction(status: Int, name: String) = WalletAction(
        WalletActionType.VALUE,
        status,
        DateUtils.timestamp,
        hashMapOf(Pair(WalletActionFields.VALUE_NAME, name))
    )
}