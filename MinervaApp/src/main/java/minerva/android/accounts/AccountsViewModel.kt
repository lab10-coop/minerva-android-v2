package minerva.android.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Space
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal

class AccountsViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = accountManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData = MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyAndHasMoreOwnersErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData

    private val _isNotSafeAccountMasterOwnerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val isNotSafeAccountMasterOwnerErrorLiveData: LiveData<Event<Throwable>> get() = _isNotSafeAccountMasterOwnerErrorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    private val _assetBalanceLiveData = MutableLiveData<Map<String, List<AccountAsset>>>()
    val accountAssetBalanceLiveData: LiveData<Map<String, List<AccountAsset>>> get() = _assetBalanceLiveData

    private val _noFundsLiveData = MutableLiveData<Event<Unit>>()
    val noFundsLiveData: LiveData<Event<Unit>> get() = _noFundsLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _accountRemovedLiveData = MutableLiveData<Event<Unit>>()
    val accountRemovedLiveData: LiveData<Event<Unit>> get() = _accountRemovedLiveData

    fun refreshBalances() =
        launchDisposable {
            transactionRepository.refreshBalances()
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

    fun refreshAssetBalance() =
        launchDisposable {
            transactionRepository.refreshAssetBalance()
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

    fun removeAccount(account: Account) {
        launchDisposable {
            accountManager.removeAccount(account.index)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(getRemovedAccountAction(account)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _accountRemovedLiveData.value = Event(Unit)},
                    onError = {
                        Timber.e("Removing account with index ${account.index} failure")
                        when (it) {
                            is BalanceIsNotEmptyAndHasMoreOwnersThrowable -> _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.value = Event(it)
                            is IsNotSafeAccountMasterOwnerThrowable -> _isNotSafeAccountMasterOwnerErrorLiveData.value = Event(it)
                            else -> _errorLiveData.value = Event(Throwable(it.message))
                        }
                    }
                )
        }
    }

    private fun getRemovedAccountAction(account: Account) =
        account.run {
            if (isSafeAccount) getWalletAction(SAFE_ACCOUNT_REMOVED, name)
            else getWalletAction(REMOVED, name)
        }

    fun createSafeAccount(account: Account) {
        if (account.cryptoBalance == BigDecimal.ZERO) {
            _noFundsLiveData.value = Event(Unit)
        } else {
            launchDisposable {
                smartContractRepository.createSafeAccount(account)
                    .flatMapCompletable { smartContractAddress -> createAccount(account, smartContractAddress) }
                    .observeOn(Schedulers.io())
                    .andThen(walletActionsRepository.saveWalletActions(getWalletAction(SAFE_ACCOUNT_ADDED, createSafeAccountName(account))))
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

    private fun createAccount(account: Account, smartContractAddress: String): Completable {
        return accountManager.createAccount(
            NetworkManager.getNetwork(account.network),
            createSafeAccountName(account),
            account.address,
            smartContractAddress
        )
    }

    private fun createSafeAccountName(account: Account): String =
        account.name.replaceFirst(String.Space, " | ${accountManager.getSafeAccountCount(account.address)} ")

    private fun getWalletAction(status: Int, name: String) = WalletAction(
        WalletActionType.ACCOUNT,
        status,
        DateUtils.timestamp,
        hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, name))
    )
}