package minerva.android.accounts.transaction.fragment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.enum.ErrorCode
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SA_ADDED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal

class AccountsViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _refreshBalancesErrorLiveData = MutableLiveData<Event<ErrorCode>>()
    val refreshBalancesErrorLiveData: LiveData<Event<ErrorCode>> get() = _refreshBalancesErrorLiveData

    private val _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData =
        MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyAndHasMoreOwnersErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData

    private val _balanceIsNotEmptyErrorLiveData = MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyErrorLiveData

    private val _isNotSafeAccountMasterOwnerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val isNotSafeAccountMasterOwnerErrorLiveData: LiveData<Event<Throwable>> get() = _isNotSafeAccountMasterOwnerErrorLiveData

    private val _automaticBackupErrorLiveData = MutableLiveData<Event<Throwable>>()
    val automaticBackupErrorLiveData: LiveData<Event<Throwable>> get() = _automaticBackupErrorLiveData

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

    val walletConfigLiveData: LiveData<WalletConfig> = accountManager.walletConfigLiveData

    private val _shouldMainNetsShowWarringLiveData = MutableLiveData<Event<Boolean>>()
    val shouldShowWarringLiveData: LiveData<Event<Boolean>> get() = _shouldMainNetsShowWarringLiveData

    private lateinit var assetVisibilitySettings: AssetVisibilitySettings
    val areMainNetsEnabled: Boolean get() = accountManager.areMainNetworksEnabled

    fun arePendingAccountsEmpty() =
        transactionRepository.getPendingAccounts().isEmpty()

    init {
        launchDisposable {
            accountManager.enableMainNetsFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _shouldMainNetsShowWarringLiveData.value = Event(it) },
                    onError = { _shouldMainNetsShowWarringLiveData.value = Event(false) })
        }
    }

    override fun onCleared() {
        super.onCleared()
        accountManager.toggleMainNetsEnabled = null
    }

    override fun onResume() {
        super.onResume()
        assetVisibilitySettings = accountManager.getAssetVisibilitySettings()
    }

    fun refreshBalances() =
        launchDisposable {
            transactionRepository.refreshBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _balanceLiveData.value = it },
                    onError = {
                        Timber.d("Refresh balance error: ${it.message}")
                        _refreshBalancesErrorLiveData.value = Event(ErrorCode.BALANCE_ERROR)
                    }
                )
        }

    fun refreshAssetBalance() =
        launchDisposable {
            transactionRepository.refreshAssetBalance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        _assetBalanceLiveData.value = it
                    },
                    onError = {
                        Timber.e("Refresh asset balance error: ${it.message}")
                        _refreshBalancesErrorLiveData.value = Event(ErrorCode.ASSET_BALANCE_ERROR)
                    }
                )
        }

    fun removeAccount(account: Account) {
        launchDisposable {
            accountManager.removeAccount(account)
                .observeOn(Schedulers.io())
                .andThen(
                    walletActionsRepository.saveWalletActions(
                        listOf(
                            getRemovedAccountAction(
                                account
                            )
                        )
                    )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _accountRemovedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Removing account with index ${account.id} failure")
                        when (it) {
                            is BalanceIsNotEmptyThrowable ->
                                _balanceIsNotEmptyErrorLiveData.value = Event(it)
                            is BalanceIsNotEmptyAndHasMoreOwnersThrowable ->
                                _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.value = Event(it)
                            is IsNotSafeAccountMasterOwnerThrowable ->
                                _isNotSafeAccountMasterOwnerErrorLiveData.value = Event(it)
                            is AutomaticBackupFailedThrowable ->
                                _automaticBackupErrorLiveData.value = Event(it)
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
                    .flatMapCompletable { smartContractAddress ->
                        accountManager.createSafeAccount(
                            account,
                            smartContractAddress
                        )
                    }
                    .observeOn(Schedulers.io())
                    .andThen(
                        walletActionsRepository.saveWalletActions(
                            listOf(
                                getWalletAction(
                                    SA_ADDED,
                                    accountManager.getSafeAccountName(account)
                                )
                            )
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

    fun addAtsToken(accounts: List<Account>, errorMessage: String) {
        getAccountForFreeATS(accounts).let { account ->
            if (account.id != Int.InvalidId) {
                transactionRepository.getFreeATS(account.address).subscribeBy(
                    onComplete = { accountManager.saveFreeATSTimestamp() },
                    onError = {
                        Timber.e("Adding 5 tATS failed: ${it.message}")
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
            } else {
                Timber.e("Adding 5 tATS failed: $errorMessage")
                _errorLiveData.value = Event(Throwable(errorMessage))
            }
        }
    }

    fun isAddingFreeATSAvailable(accounts: List<Account>): Boolean =
        accountManager.shouldGetFreeAts() &&
                accounts.any { it.network.short == NetworkManager.networks[FIRST_DEFAULT_NETWORK_INDEX].short }


    @VisibleForTesting
    fun getAccountForFreeATS(accounts: List<Account>): Account {
        accounts.forEach {
            if (it.network.short == NetworkManager.networks[FIRST_DEFAULT_NETWORK_INDEX].short)
                return it
        }
        return Account(Int.InvalidId)
    }

    fun isAssetVisible(networkAddress: String, assetAddress: String) =
        assetVisibilitySettings.getAssetVisibility(networkAddress, assetAddress)

    fun saveAssetVisible(networkAddress: String, assetAddress: String, visibility: Boolean) {
        assetVisibilitySettings = accountManager.saveAssetVisibilitySettings(
            assetVisibilitySettings.updateAssetVisibility(networkAddress, assetAddress, visibility)
        )
    }

    private fun getWalletAction(status: Int, name: String) =
        WalletAction(
            WalletActionType.ACCOUNT,
            status,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, name))
        )
}