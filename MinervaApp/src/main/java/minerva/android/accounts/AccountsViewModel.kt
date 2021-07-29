package minerva.android.accounts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.CompositeException
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.state.*
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
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
import minerva.android.walletmanager.model.minervaprimitives.account.*
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
    private val rawAccounts: List<Account> get() = accountManager.rawAccounts
    private val cachedTokens: Map<Int, List<ERC20Token>> get() = accountManager.cachedTokens
    var tokenVisibilitySettings: TokenVisibilitySettings = accountManager.getTokenVisibilitySettings
    val areMainNetsEnabled: Boolean get() = accountManager.areMainNetworksEnabled
    val isProtectKeysEnabled: Boolean get() = accountManager.isProtectKeysEnabled
    val arePendingAccountsEmpty: Boolean
        get() = transactionRepository.getPendingAccounts().isEmpty() && rawAccounts.any { account -> account.isPending }
    val isSynced: Boolean get() = walletActionsRepository.isSynced
    val isRefreshDone: Boolean get() = coinBalancesRefreshed && tokenBalancesRefreshed
    private var coinBalancesRefreshed = false
    private var tokenBalancesRefreshed = false
    val fiatSymbol: String = transactionRepository.getFiatSymbol()
    val isFirstLaunch: Boolean get() = accountManager.areAllEmptyMainNetworkAccounts()
    val accountsLiveData: LiveData<List<Account>> =
        Transformations.map(accountManager.walletConfigLiveData) { event -> event.peekContent().accounts }

    private val _errorLiveData = MutableLiveData<Event<AccountsErrorState>>()
    val errorLiveData: LiveData<Event<AccountsErrorState>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _balanceStateLiveData = MutableLiveData<BalanceState>()
    val balanceStateLiveData: LiveData<BalanceState> get() = _balanceStateLiveData

    private val _accountHideLiveData = MutableLiveData<Event<Unit>>()
    val accountHideLiveData: LiveData<Event<Unit>> get() = _accountHideLiveData

    private val _addFreeAtsLiveData = MutableLiveData<Event<Boolean>>()
    val addFreeAtsLiveData: LiveData<Event<Boolean>> get() = _addFreeAtsLiveData

    private val _dappSessions = MutableLiveData<List<DappSessionData>>()
    val dappSessions: LiveData<List<DappSessionData>> get() = _dappSessions

    override fun onResume() {
        super.onResume()
        tokenVisibilitySettings = accountManager.getTokenVisibilitySettings
        refreshCoinBalances()
        refreshTokensBalances()
        discoverNewTokens()
        getSessions(accountManager.getAllAccounts())
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

    private fun updateSessions(sessions: List<DappSession>, accounts: List<Account>): List<DappSessionData> =
        if (sessions.isNotEmpty()) {
            mutableListOf<DappSessionData>().apply {
                accounts.forEach { account ->
                    if (isCurrentSession(sessions, account)) {
                        val count =
                            sessions.count { session ->
                                session.address.equals(account.address, true) && session.chainId == account.chainId
                            }
                        add(DappSessionData(account.address, account.chainId, count))
                    }
                }
            }
        } else emptyList()

    private fun isCurrentSession(sessions: List<DappSession>, account: Account) =
        sessions.find { session ->
            session.address.equals(account.address, true) && session.chainId == account.chainId
        } != null

    fun hideAccount(index: Int) {
        val account = rawAccounts[index]
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
            coinBalancesRefreshed = false
            transactionRepository.getCoinBalance()
                .map { coin ->
                    when (coin) {
                        is CoinBalance -> getAccountIndexToUpdate(coin)
                        is CoinError -> getAccountIndexWithError(coin)
                        else -> Int.InvalidIndex
                    }
                }
                .filter { index -> index != Int.InvalidIndex }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        coinBalancesRefreshed = true
                        _balanceStateLiveData.value = CoinBalanceCompleted
                    },
                    onNext = { index -> _balanceStateLiveData.value = CoinBalanceUpdate(index) },
                    onError = { error ->
                        logError("Refresh balance error: ${getError(error)}")
                        _errorLiveData.value = Event(RefreshBalanceError)
                    }
                )
        }

    private fun getAccountIndexWithError(coin: CoinError): Int {
        logError("Refresh balance error: ${coin.error}")
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (isAccountToUpdate(account, coin) && !account.isError) {
                    account.isError = true
                    return index
                }
            }
        return Int.InvalidIndex
    }

    private fun getAccountIndexToUpdate(coinBalance: CoinBalance): Int {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (shouldUpdateCoinBalance(account, coinBalance)) {
                    account.cryptoBalance = coinBalance.balance.cryptoBalance
                    account.fiatBalance = coinBalance.balance.fiatBalance
                    account.coinRate = coinBalance.rate ?: Double.InvalidValue
                    account.isError = false
                    return index
                } else if (isAccountToUpdate(account, coinBalance) && account.isError) {
                    account.isError = false
                    return index
                }
            }
        return Int.InvalidIndex
    }

    private fun shouldUpdateCoinBalance(account: Account, coinBalance: CoinBalance): Boolean =
        isAccountToUpdate(account, coinBalance) &&
                (account.cryptoBalance != coinBalance.balance.cryptoBalance || account.fiatBalance != coinBalance.balance.fiatBalance)

    private fun isAccountToUpdate(account: Account, coinBalance: Coin) =
        (account.address.equals(coinBalance.address, true) && account.chainId == coinBalance.chainId)

    fun refreshTokensBalances() {
        launchDisposable {
            tokenBalancesRefreshed = false
            transactionRepository.getTaggedTokensUpdate()
                .filter { taggedTokens -> taggedTokens.isNotEmpty() }
                .doOnNext {
                    transactionRepository.getTokenBalance()
                        .map { asset ->
                            when (asset) {
                                is AssetBalance -> updateTokenAndReturnIndex(asset)
                                is AssetError -> updateTokenErrorAndReturnIndex(asset)
                                else -> Int.InvalidIndex
                            }
                        }
                        .filter { index -> index != Int.InvalidIndex }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onComplete = {
                                tokenBalancesRefreshed = true
                                _balanceStateLiveData.value = TokenBalanceCompleted
                            },
                            onNext = { index -> _balanceStateLiveData.value = TokenBalanceUpdate(index) },
                            onError = { error ->
                                logError("Refresh tokens error: ${getError(error)}")
                                _errorLiveData.value = Event(RefreshBalanceError)
                            }
                        )
                }.subscribe()
        }
    }

    private fun getError(error: Throwable): String =
        if (error is CompositeException) {
            "${error.exceptions}"
        } else {
            "$error"
        }

    fun getTokens(account: Account): List<ERC20Token> =
        if (account.accountTokens.isNotEmpty()) {
            account.accountTokens
                .filter { accountToken -> shouldShowAccountToken(account, accountToken) }
                .sortedByDescending { accountToken -> accountToken.fiatBalance }
                .map { accountToken -> accountToken.token }
                .toList()
        } else {
            cachedTokens[account.chainId]
                ?.filter { token -> shouldShowCachedToken(token, account) }
                ?.apply {
                    if (account.accountTokens.isEmpty()) {
                        forEach { erc20Token -> account.accountTokens.add(AccountToken(token = erc20Token)) }
                    }
                }
                ?: emptyList()
        }

    private fun shouldShowCachedToken(token: ERC20Token, account: Account): Boolean =
        token.accountAddress.equals(account.address, true) &&
                tokenVisibilitySettings.getTokenVisibility(account.address, token.address) == true

    private fun shouldShowAccountToken(account: Account, accountToken: AccountToken): Boolean =
        tokenVisibilitySettings.getTokenVisibility(account.address, accountToken.token.address) == true &&
                (hasFunds(accountToken.balance) || accountToken.token.isError)

    private fun updateTokenErrorAndReturnIndex(balance: AssetError): Int {
        var accountIndex: Int = Int.InvalidIndex
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (isTokenInAccount(balance, account)) {
                    val tokenAddress = balance.tokenAddress
                    val accountAddress = balance.accountAddress
                    account.accountTokens.forEach { accountToken ->
                        if (isTheSameToken(accountToken, tokenAddress, accountAddress) && !accountToken.token.isError) {
                            accountToken.token.isError = true
                            accountIndex = index
                        }
                    }
                }
            }
        return accountIndex
    }

    private fun updateTokenAndReturnIndex(balance: AssetBalance): Int {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (isTokenInAccount(balance, account)) {
                    return updateTokenVisibilityAndReturnIndex(account, balance, index)
                }
            }
        return Int.InvalidIndex
    }

    private fun isTokenInAccount(balance: Asset, account: Account): Boolean =
        balance.chainId == account.chainId && balance.privateKey.equals(account.privateKey, true)

    private fun updateTokenVisibilityAndReturnIndex(account: Account, asset: AssetBalance, index: Int): Int {
        var accountIndex: Int = Int.InvalidIndex
        isTokenVisible(account.address, asset.accountToken)?.let { isVisible ->
            accountIndex = if (isVisible) {
                updateVisibleTokenAndReturnIndex(account, asset, index)
            } else {
                updateNotVisibleTokenAndReturnIndex(account, asset, index)
            }
        }.orElse {
            accountIndex = updateNewTokenAndReturnIndex(account, asset, index)
        }
        return accountIndex
    }

    fun isTokenVisible(accountAddress: String, accountToken: AccountToken): Boolean? =
        tokenVisibilitySettings.getTokenVisibility(accountAddress, accountToken.token.address)

    private fun updateNewTokenAndReturnIndex(account: Account, balance: AssetBalance, index: Int): Int {
        var accountIndex: Int = Int.InvalidIndex
        if (hasFunds(balance.accountToken.balance)) {
            saveTokenVisible(account.address, balance.accountToken.token.address, true)
            account.accountTokens.add(balance.accountToken)
            accountIndex = index
        }
        return accountIndex
    }

    private fun updateNotVisibleTokenAndReturnIndex(account: Account, balance: AssetBalance, index: Int): Int {
        var accountIndex: Int = Int.InvalidIndex
        if (isTokenShown(account, balance.accountToken.token)) {
            account.accountTokens.remove(balance.accountToken)
            accountIndex = index
        }
        return accountIndex
    }

    private fun updateVisibleTokenAndReturnIndex(account: Account, balance: AssetBalance, index: Int): Int {
        var accountIndex: Int = Int.InvalidIndex
        if (isTokenShown(account, balance.accountToken.token)) {
            val tokenAddress = balance.accountToken.token.address
            val accountAddress = balance.accountToken.token.accountAddress
            account.accountTokens.find { token -> isTheSameToken(token, tokenAddress, accountAddress) }
                ?.let { accountToken ->
                    if (shouldUpdateBalance(accountToken, balance)) {
                        accountToken.rawBalance = balance.accountToken.rawBalance
                        accountToken.tokenPrice = balance.accountToken.tokenPrice
                        accountToken.token.isError = false
                        accountIndex = index
                    } else if (accountToken.token.isError) {
                        accountToken.token.isError = false
                        accountIndex = index
                    }
                }
        } else {
            account.accountTokens.add(balance.accountToken)
            accountIndex = index
        }
        return accountIndex
    }

    private fun shouldUpdateBalance(accountToken: AccountToken?, balance: AssetBalance) =
        accountToken?.rawBalance != balance.accountToken.rawBalance ||
                accountToken.tokenPrice != balance.accountToken.tokenPrice

    private fun isTokenShown(account: Account, token: ERC20Token) =
        account.accountTokens.find { accountToken ->
            isTheSameToken(accountToken, token.address, token.accountAddress)
        } != null

    private fun isTheSameToken(accountToken: AccountToken, tokenAddress: String, accountAddress: String): Boolean =
        accountToken.token.address.equals(tokenAddress, true) &&
                accountToken.token.accountAddress.equals(accountAddress, true)

    private fun hasFunds(balance: BigDecimal) = balance > BigDecimal.ZERO

    fun updateTokensRate() {
        transactionRepository.updateTokensRate()
        _balanceStateLiveData.value = UpdateAllState
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

    private fun getSafeAccountWalletAction(account: Account): List<WalletAction> =
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

    fun updateSessionCount(sessionsPerAccount: List<DappSessionData>, passIndex: (index: Int) -> Unit) {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                account.apply {
                    dappSessionCount = sessionsPerAccount
                        .find { dappSession ->
                            dappSession.address.equals(address, true) && dappSession.chainId == chainId
                        }?.count ?: NO_DAPP_SESSION
                    passIndex(index)
                }
            }
    }

    fun showPendingAccount(
        index: Int,
        chainId: Int,
        areMainNetsEnabled: Boolean,
        isPending: Boolean,
        passIndex: (index: Int) -> Unit
    ) {
        rawAccounts.forEachIndexed { position, account ->
            if (shouldShowPending(account, index, chainId, areMainNetsEnabled)) {
                account.isPending = isPending
                passIndex(position)
            }
        }
    }

    fun indexOfRawAccounts(account: Account): Int = rawAccounts.indexOf(account)
    fun stopPendingAccounts() {
        rawAccounts.forEach { account -> account.isPending = false }
    }

    private fun shouldShowPending(account: Account, index: Int, chainId: Int, areMainNetsEnabled: Boolean): Boolean =
        account.id == index && account.chainId == chainId && account.network.testNet != areMainNetsEnabled

    companion object {
        private const val DAY: Long = 24L
        private const val NO_DAPP_SESSION = 0
    }
}
