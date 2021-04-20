package minerva.android.accounts.transaction.fragment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.enum.ErrorCode
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyAndHasMoreOwnersThrowable
import minerva.android.walletmanager.exception.BalanceIsNotEmptyThrowable
import minerva.android.walletmanager.exception.IsNotSafeAccountMasterOwnerThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SAFE_ACCOUNT_REMOVED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SA_ADDED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.widget.state.AccountWidgetState
import minerva.android.widget.state.AppUIState
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class AccountsViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository,
    private val appUIState: AppUIState
) : BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _refreshBalancesErrorLiveData = MutableLiveData<Event<ErrorCode>>()
    val refreshBalancesErrorLiveData: LiveData<Event<ErrorCode>> get() = _refreshBalancesErrorLiveData

    private val _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData = MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyAndHasMoreOwnersErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData

    private val _balanceIsNotEmptyErrorLiveData = MutableLiveData<Event<Throwable>>()
    val balanceIsNotEmptyErrorLiveData: LiveData<Event<Throwable>> get() = _balanceIsNotEmptyErrorLiveData

    private val _isNotSafeAccountMasterOwnerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val isNotSafeAccountMasterOwnerErrorLiveData: LiveData<Event<Throwable>> get() = _isNotSafeAccountMasterOwnerErrorLiveData

    private val _automaticBackupErrorLiveData = MutableLiveData<Event<Throwable>>()
    val automaticBackupErrorLiveData: LiveData<Event<Throwable>> get() = _automaticBackupErrorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    private val _tokenBalanceLiveData = MutableLiveData<Unit>()
    val tokenBalanceLiveData: LiveData<Unit> get() = _tokenBalanceLiveData

    private val _noFundsLiveData = MutableLiveData<Event<Unit>>()
    val noFundsLiveData: LiveData<Event<Unit>> get() = _noFundsLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _accountRemovedLiveData = MutableLiveData<Event<Unit>>()
    val accountRemovedLiveData: LiveData<Event<Unit>> get() = _accountRemovedLiveData

    private val _addFreeAtsLiveData = MutableLiveData<Event<Boolean>>()
    val addFreeAtsLiveData: LiveData<Event<Boolean>> get() = _addFreeAtsLiveData

    private val _dappSessions = MutableLiveData<HashMap<String, Int>>()
    val dappSessions: LiveData<HashMap<String, Int>> get() = _dappSessions
    var hasAvailableAccounts: Boolean = false
    var activeAccounts: List<Account> = listOf()

    val accountsLiveData: LiveData<List<Account>> =
        Transformations.map(accountManager.walletConfigLiveData) {
            with(it.peekContent()) {
                hasAvailableAccounts = hasActiveAccount
                activeAccounts = accounts.filter { !it.isDeleted && it.network.testNet == !areMainNetsEnabled }
                accounts
            }
        }

    var showMainNetworksWarning: Boolean
        get() = accountManager.showMainNetworksWarning
        set(value) {
            accountManager.showMainNetworksWarning = value
        }

    @VisibleForTesting
    lateinit var tokenVisibilitySettings: TokenVisibilitySettings
    val areMainNetsEnabled: Boolean get() = accountManager.areMainNetworksEnabled

    private var balancesRefreshed = false
    private var tokenBalancesRefreshed = false

    fun arePendingAccountsEmpty() = transactionRepository.getPendingAccounts().isEmpty()

    override fun onResume() {
        super.onResume()
        tokenVisibilitySettings = accountManager.getTokenVisibilitySettings()
        refreshBalances()
        refreshTokenBalance()
        refreshTokensList()
        accountManager.getAllAccounts()?.let { getSessions(it) }
    }

    fun getFiatSymbol() = transactionRepository.getFiatSymbol()

    fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        appUIState.updateAccountWidgetState(index, accountWidgetState)

    fun getAccountWidgetState(index: Int) = appUIState.getAccountWidgetState(index)

    internal fun getSessions(accounts: List<Account>) {
        launchDisposable {
            walletConnectRepository.getSessionsFlowable()
                .map { sessions -> updateSessions(sessions, accounts) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _dappSessions.value = it },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun isAuthenticationEnabled() = accountManager.isAuthenticationEnabled

    private fun updateSessions(sessions: List<DappSession>, accounts: List<Account>): HashMap<String, Int> =
        if (sessions.isNotEmpty()) {
            hashMapOf<String, Int>().apply {
                accounts.forEach { account ->
                    if (isCurrentSession(sessions, account)) {
                        val count = sessions.count { it.address == accountManager.toChecksumAddress(account.address) }
                        this[account.address] = count
                    }
                }
            }
        } else hashMapOf()

    private fun isCurrentSession(sessions: List<DappSession>, account: Account) =
        sessions.find { it.address == accountManager.toChecksumAddress(account.address) } != null

    fun removeAccount(account: Account) {
        launchDisposable {
            accountManager.removeAccount(account)
                .observeOn(Schedulers.io())
                .andThen(walletConnectRepository.killAllAccountSessions(accountManager.toChecksumAddress(account.address)))
                .andThen(walletActionsRepository.saveWalletActions(listOf(getRemovedAccountAction(account))))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _accountRemovedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Removing account with index ${account.id} failure")
                        handleRemoveAccountErrors(it)
                    }
                )
        }
    }

    fun refreshBalances() =
        launchDisposable {
            balancesRefreshed = false
            transactionRepository.refreshBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        balancesRefreshed = true
                        _balanceLiveData.value = it
                    },
                    onError = {
                        Timber.d("Refresh balance error: ${it.message}")
                        _refreshBalancesErrorLiveData.value = Event(ErrorCode.BALANCE_ERROR)
                    }
                )
        }


    private fun filterNotVisibleTokens(accountTokenBalances: Map<String, List<AccountToken>>): Map<String, List<AccountToken>> {
        activeAccounts.filter { !it.isPending }.forEach { account ->
            accountTokenBalances[account.privateKey]?.let { tokensList ->
                account.accountTokens = tokensList.filter {
                    isTokenVisible(account.address, it).orElse {
                        saveTokenVisible(account.address, it.token.address, true)
                        hasFunds(it.balance)
                    }
                }
            }
        }
        return accountTokenBalances
    }

    fun refreshTokenBalance() =
        launchDisposable {
            tokenBalancesRefreshed = false
            transactionRepository.refreshTokenBalance()
                .map { filterNotVisibleTokens(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        tokenBalancesRefreshed = true
                        _tokenBalanceLiveData.value = Unit
                    },
                    onError = {
                        Timber.e(it)
                        _refreshBalancesErrorLiveData.value = Event(ErrorCode.TOKEN_BALANCE_ERROR)
                    }
                )
        }

    fun updateTokensRate() {
        transactionRepository.updateTokensRate()
        _tokenBalanceLiveData.value = Unit
    }

    fun refreshTokensList() =
        launchDisposable {
            transactionRepository.refreshTokensList()
                .filter { it }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { refreshTokenBalance() },
                    onError = { Timber.e(it) }
                )
        }

    fun isRefreshDone() = balancesRefreshed && tokenBalancesRefreshed

    private fun handleRemoveAccountErrors(it: Throwable) {
        when (it) {
            is BalanceIsNotEmptyThrowable -> _balanceIsNotEmptyErrorLiveData.value = Event(it)
            is BalanceIsNotEmptyAndHasMoreOwnersThrowable -> _balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.value =
                Event(it)
            is IsNotSafeAccountMasterOwnerThrowable -> _isNotSafeAccountMasterOwnerErrorLiveData.value = Event(it)
            is AutomaticBackupFailedThrowable -> _automaticBackupErrorLiveData.value = Event(it)
            else -> _errorLiveData.value = Event(Throwable(it.message))
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

    fun addAtsToken() {
        getAccountForFreeATS(activeAccounts).let { account ->
            if (account.id != Int.InvalidId && isAddingFreeATSAvailable(activeAccounts)) {
                launchDisposable {
                    transactionRepository.getFreeATS(account.address)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onComplete = {
                                accountManager.saveFreeATSTimestamp()
                                _addFreeAtsLiveData.value = Event(true)
                            },
                            onError = {
                                Timber.e("Adding 5 tATS failed: ${it.message}")
                                _errorLiveData.value = Event(Throwable(it.message))
                            }
                        )
                }
            } else _addFreeAtsLiveData.value = Event(false)
        }
    }

    fun isAddingFreeATSAvailable(accounts: List<Account>): Boolean =
        shouldGetFreeAts() &&
                accounts.any { it.network.chainId == NetworkManager.networks[FIRST_DEFAULT_NETWORK_INDEX].chainId }

    private fun shouldGetFreeAts() =
        ((accountManager.getLastFreeATSTimestamp() + TimeUnit.HOURS.toMillis(24L)) < accountManager.currentTimeMills())

    @VisibleForTesting
    fun getAccountForFreeATS(accounts: List<Account>): Account {
        accounts.forEach {
            if (it.network.chainId == NetworkManager.networks[FIRST_DEFAULT_NETWORK_INDEX].chainId)
                return it
        }
        return Account(Int.InvalidId)
    }

    fun isTokenVisible(networkAddress: String, accountToken: AccountToken) =
        tokenVisibilitySettings.getTokenVisibility(networkAddress, accountToken.token.address)?.let {
            it && hasFunds(accountToken.balance)
        }

    private fun hasFunds(balance: BigDecimal) = balance > BigDecimal.ZERO

    private fun saveTokenVisible(networkAddress: String, tokenAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = accountManager.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(networkAddress, tokenAddress, visibility)
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