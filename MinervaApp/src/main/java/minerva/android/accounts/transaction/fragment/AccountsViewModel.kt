package minerva.android.accounts.transaction.fragment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
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
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_TEST_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.HIDE
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SA_ADDED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.minervaprimitives.account.TokenBalance
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class AccountsViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository,
    private val logger: Logger
) : BaseViewModel() {
    val hasAvailableAccounts: Boolean get() = accountManager.hasAvailableAccounts
    val activeAccounts: List<Account> get() = accountManager.activeAccounts
    private val cachedTokens: Map<Int, List<ERC20Token>> get() = accountManager.cachedTokens
    var tokenVisibilitySettings: TokenVisibilitySettings = accountManager.getTokenVisibilitySettings
    val areMainNetsEnabled: Boolean get() = accountManager.areMainNetworksEnabled
    val isProtectKeysEnabled: Boolean get() = accountManager.isProtectKeysEnabled
    val arePendingAccountsEmpty: Boolean get() = transactionRepository.getPendingAccounts().isEmpty()
    val isSynced: Boolean get() = walletActionsRepository.isSynced
    val isRefreshDone: Boolean get() = balancesRefreshed && tokenBalancesRefreshed
    private var balancesRefreshed = false
    private var tokenBalancesRefreshed = false
    val fiatSymbol = transactionRepository.getFiatSymbol()
    val isFirstLaunch: Boolean get() = accountManager.areAllEmptyMainNetworkAccounts()

    val accountsLiveData: LiveData<List<Account>> =
        Transformations.map(accountManager.walletConfigLiveData) { event -> event.peekContent().accounts }

    private val _errorLiveData = MutableLiveData<Event<AccountsErrorState>>()
    val errorLiveData: LiveData<Event<AccountsErrorState>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _balanceLiveData = MutableLiveData<List<CoinBalance>>()
    val balanceLiveData: LiveData<List<CoinBalance>> get() = _balanceLiveData

    private val _tokenBalanceLiveData = MutableLiveData<Unit>()
    val tokenBalanceLiveData: LiveData<Unit> get() = _tokenBalanceLiveData

    private val _accountHideLiveData = MutableLiveData<Event<Unit>>()
    val accountHideLiveData: LiveData<Event<Unit>> get() = _accountHideLiveData

    private val _addFreeAtsLiveData = MutableLiveData<Event<Boolean>>()
    val addFreeAtsLiveData: LiveData<Event<Boolean>> get() = _addFreeAtsLiveData

    private val _dappSessions = MutableLiveData<HashMap<String, Int>>()
    val dappSessions: LiveData<HashMap<String, Int>> get() = _dappSessions

    override fun onResume() {
        super.onResume()
        tokenVisibilitySettings = accountManager.getTokenVisibilitySettings
        refreshCoinBalances()
        refreshTokensBalances()
        discoverNewTokens()
        getSessions(accountManager.getAllAccounts())
    }

    fun getTokens(account: Account): List<ERC20Token> =
        if (account.accountTokens.isNotEmpty()) {
            account.accountTokens
                .sortedByDescending { accountToken -> accountToken.fiatBalance }
                .map { accountToken -> accountToken.token }.toList()
        } else {
            cachedTokens[account.chainId]
                ?.filter { token ->
                    token.accountAddress.equals(account.address, true) &&
                            tokenVisibilitySettings.getTokenVisibility(account.address, token.address) == true
                } ?: emptyList()
        }

    internal fun getSessions(accounts: List<Account>) {
        launchDisposable {
            walletConnectRepository.getSessionsFlowable()
                .map { sessions -> updateSessions(sessions, accounts) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { sessionsPerAccount -> _dappSessions.value = sessionsPerAccount },
                    onError = { _errorLiveData.value = Event(BaseError) }
                )
        }
    }

    private fun updateSessions(sessions: List<DappSession>, accounts: List<Account>): HashMap<String, Int> =
        if (sessions.isNotEmpty()) {
            hashMapOf<String, Int>().apply {
                accounts.forEach { account ->
                    if (isCurrentSession(sessions, account)) {
                        val count =
                            sessions.count { session -> session.address == accountManager.toChecksumAddress(account.address) }
                        this[account.address] = count
                    }
                }
            }
        } else hashMapOf()

    private fun isCurrentSession(sessions: List<DappSession>, account: Account) =
        sessions.find { session -> session.address == accountManager.toChecksumAddress(account.address) } != null

    fun hideAccount(account: Account) {
        launchDisposable {
            accountManager.hideAccount(account)
                .observeOn(Schedulers.io())
                .andThen(walletConnectRepository.killAllAccountSessions(accountManager.toChecksumAddress(account.address)))
                .andThen(walletActionsRepository.saveWalletActions(listOf(getHideAccountAction(account))))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _accountHideLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Hiding account with index ${account.id} failure")
                        handleHideAccountErrors(it)
                    }
                )
        }
    }

    fun refreshCoinBalances() =
        launchDisposable {
            balancesRefreshed = false
            transactionRepository.refreshCoinBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { balancePerAccountMap ->
                        balancesRefreshed = true
                        _balanceLiveData.value = balancePerAccountMap
                    },
                    onError = { error ->
                        logError("Refresh balance error: $error")
                        _errorLiveData.value = Event(RefreshCoinBalancesError)
                    }
                )
        }

    fun refreshTokensBalances() =
        launchDisposable {
            tokenBalancesRefreshed = false
            transactionRepository.getTaggedTokensUpdate()
                .filter { taggedTokens -> taggedTokens.isNotEmpty() }
                .switchMapSingle { transactionRepository.refreshTokensBalances() }
                .map { accountTokensMap -> filterNotVisibleTokens(accountTokensMap) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        tokenBalancesRefreshed = true
                        _tokenBalanceLiveData.value = Unit
                    },
                    onError = { error ->
                        logError("Refresh tokens error: $error")
                        _errorLiveData.value = Event(RefreshTokenBalancesError)
                    }
                )
        }

    private fun filterNotVisibleTokens(accountTokenBalances: List<TokenBalance>) {
        activeAccounts.filter { account -> !account.isPending }
            .forEach { account ->
                accountTokenBalances.find { tokenBalance ->
                    tokenBalance.chainId == account.chainId && tokenBalance.privateKey == account.privateKey
                }?.accountTokenList?.let { tokensList ->
                    account.accountTokens = tokensList.filter { accountToken ->
                        isTokenVisible(account.address, accountToken).orElse {
                            saveTokenVisible(account.address, accountToken.token.address, true)
                            hasFunds(accountToken.balance)
                        }
                    }
                }
            }
    }

    fun isTokenVisible(accountAddress: String, accountToken: AccountToken): Boolean? =
        tokenVisibilitySettings.getTokenVisibility(accountAddress, accountToken.token.address)
            ?.let { isTokenVisible -> isTokenVisible && hasFunds(accountToken.balance) }

    private fun hasFunds(balance: BigDecimal) = balance > BigDecimal.ZERO

    fun updateTokensRate() {
        transactionRepository.updateTokensRate()
        _tokenBalanceLiveData.value = Unit
    }

    fun discoverNewTokens() =
        launchDisposable {
            transactionRepository.discoverNewTokens()
                .filter { shouldRefreshTokensBalances -> shouldRefreshTokensBalances }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { refreshTokensBalances() },
                    onError = { error -> logError("Error while token auto-discovery: $error") }
                )
        }

    private fun logError(message: String) {
        logger.logToFirebase(message)
        Timber.d(message)
    }

    private fun handleHideAccountErrors(error: Throwable) {
        logError("Error while hiding account: $error")
        _errorLiveData.value = when (error) {
            is BalanceIsNotEmptyThrowable -> Event(BalanceIsNotEmptyError)
            is BalanceIsNotEmptyAndHasMoreOwnersThrowable -> Event(BalanceIsNotEmptyAndHasMoreOwnersError)
            is IsNotSafeAccountMasterOwnerThrowable -> Event(IsNotSafeAccountMasterOwnerError)
            is AutomaticBackupFailedThrowable -> Event(AutomaticBackupError(error))
            else -> Event(BaseError)
        }
    }

    private fun getHideAccountAction(account: Account) =
        account.run {
            getWalletAction(HIDE, name)
        }

    fun createSafeAccount(account: Account) {
        if (account.cryptoBalance == BigDecimal.ZERO) {
            _errorLiveData.value = Event(NoFunds)
        } else {
            launchDisposable {
                smartContractRepository.createSafeAccount(account)
                    .flatMapCompletable { smartContractAddress ->
                        accountManager.createSafeAccount(account, smartContractAddress)
                    }
                    .observeOn(Schedulers.io())
                    .andThen(walletActionsRepository.saveWalletActions(getSafeAccountWalletAction(account)))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { _loadingLiveData.value = Event(true) }
                    .doOnEvent { _loadingLiveData.value = Event(false) }
                    .subscribeBy(
                        onError = {
                            Timber.e("Creating safe account error: ${it.message}")
                            _errorLiveData.value = Event(BaseError)
                        }
                    )
            }
        }
    }

    private fun getSafeAccountWalletAction(account: Account) =
        listOf(getWalletAction(SA_ADDED, accountManager.getSafeAccountName(account)))

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
                                _errorLiveData.value = Event(BaseError)
                            }
                        )
                }
            } else _addFreeAtsLiveData.value = Event(false)
        }
    }

    fun isAddingFreeATSAvailable(accounts: List<Account>): Boolean =
        shouldGetFreeAts() && accounts.any { account -> account.network.chainId == NetworkManager.networks[FIRST_DEFAULT_TEST_NETWORK_INDEX].chainId }

    private fun shouldGetFreeAts() =
        ((accountManager.getLastFreeATSTimestamp() + TimeUnit.HOURS.toMillis(DAY)) < accountManager.currentTimeMills())

    @VisibleForTesting
    fun getAccountForFreeATS(accounts: List<Account>): Account =
        accounts.find { account ->
            account.network.chainId == NetworkManager.networks[FIRST_DEFAULT_TEST_NETWORK_INDEX].chainId
        } ?: Account(Int.InvalidId)

    private fun saveTokenVisible(networkAddress: String, tokenAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = accountManager.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(networkAddress, tokenAddress, visibility)
        )
    }

    fun createNewAccount(chainId: Int) {
        launchDisposable {
            accountManager.createRegularAccount(NetworkManager.getNetwork(chainId))
                .flatMapCompletable {
                    walletActionsRepository.saveWalletActions(listOf(getWalletAction(WalletActionStatus.ADDED, it)))
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(BaseError)
                    }
                )
        }
    }

    private fun getWalletAction(status: Int, name: String) =
        WalletAction(
            WalletActionType.ACCOUNT,
            status,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, name))
        )

    fun changeAccountName(account: Account, newName: String) {
        launchDisposable {
            accountManager.changeAccountName(account, newName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(BaseError)
                    }
                )
        }
    }

    companion object {
        private const val DAY: Long = 24L
    }
}
