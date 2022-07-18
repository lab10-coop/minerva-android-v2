package minerva.android.accounts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.extensions.*
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
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_DEFAULT_TEST_NETWORK_INDEX
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.HIDE
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SA_ADDED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ActiveSuperToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class AccountsViewModel(
    private val accountManager: AccountManager,
    private val tokenManager: TokenManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val safeAccountRepository: SafeAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository,
    private val logger: Logger
) : BaseViewModel() {
    val hasAvailableAccounts: Boolean get() = accountManager.hasAvailableAccounts
    val activeAccounts: List<Account> get() = accountManager.activeAccounts
    private val rawAccounts: List<Account> get() = accountManager.rawAccounts
    private val cachedTokens: Map<Int, List<ERCToken>> get() = accountManager.cachedTokens
    private var cachedAccountTokens: MutableList<AccountToken> = mutableListOf()
    private var newTokens: MutableList<AccountToken> = mutableListOf()
    private var newCachedTokens: MutableList<AccountToken> = mutableListOf()
    var tokenVisibilitySettings: TokenVisibilitySettings = accountManager.getTokenVisibilitySettings
    val areMainNetsEnabled: Boolean get() = accountManager.areMainNetworksEnabled
    val isProtectKeysEnabled: Boolean get() = accountManager.isProtectKeysEnabled
    val arePendingAccountsEmpty: Boolean
        get() = transactionRepository.getPendingAccounts()
            .isEmpty() && rawAccounts.any { account -> account.isPending }
    val isSynced: Boolean get() = walletActionsRepository.isSynced
    val isRefreshDone: Boolean get() = coinBalancesRefreshed && tokenBalancesRefreshed
    private var coinBalancesRefreshed = false
    private var tokenBalancesRefreshed = false
    val fiatSymbol: String = transactionRepository.getFiatSymbol()
    val isFirstLaunch: Boolean get() = accountManager.areAllEmptyMainNetworkAccounts()

    private val _accountsLiveData = MutableLiveData<List<Account>>(activeAccounts)
    val accountsLiveData: LiveData<List<Account>> get() = _accountsLiveData

    val mediatorLiveData = MediatorLiveData<List<Account>>().apply {
        addSource(
            accountManager.walletConfigLiveData
        ) {
            _accountsLiveData.value = activeAccounts
        }
        addSource(
            accountManager.balancesInsertLiveData
        ) {
            _accountsLiveData.value = activeAccounts
        }
        addSource(
            transactionRepository.ratesMapLiveData
        ) {
            _accountsLiveData.value = activeAccounts
        }
    }

    private lateinit var streamingDisposable: CompositeDisposable

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

    private fun fetchNFTData(){
        launchDisposable {
            tokenManager.fetchNFTsDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                    }
                )
        }
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

    private fun updateSessions(
        sessions: List<DappSession>,
        accounts: List<Account>
    ): List<DappSessionData> =
        if (sessions.isNotEmpty()) {
            mutableListOf<DappSessionData>().apply {
                accounts.forEach { account ->
                    if (isCurrentSession(sessions, account)) {
                        val count =
                            sessions.count { session ->
                                session.address.equals(
                                    account.address,
                                    true
                                ) && session.chainId == account.chainId
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
                .andThen(
                    walletConnectRepository.killAllAccountSessions(
                        accountManager.toChecksumAddress(account.address, account.chainId),
                        account.chainId
                    )
                )
                .andThen(
                    walletActionsRepository.saveWalletActions(
                        listOf(
                            getHideAccountAction(
                                account
                            )
                        )
                    )
                )
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

    fun getTokens(account: Account): List<AccountToken> {
        val tokens = if (account.accountTokens.isEmpty()) {
            cachedAccountTokens
                .filter { accountToken ->
                    accountToken.shouldShowCachedToken(account, tokenVisibilitySettings)
                }

        } else {
            account.accountTokens
                .filter { accountToken ->
                    accountToken.shouldShowAccountToken(account, tokenVisibilitySettings)
                }
        }
        return tokens.sortedByDescending { accountToken -> accountToken.fiatBalance }
    }

    fun refreshCoinBalances() =
        launchDisposable {
            coinBalancesRefreshed = false
            transactionRepository.getCoinBalance()
                .flatMap { coin ->
                    when (coin) {
                        is CoinBalance -> getAccountIndexToUpdate(coin)
                        is CoinError -> getAccountIndexWithError(coin)
                        else -> Flowable.just(Int.InvalidIndex)
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
                        logError("Refresh coin balance error: ${getError(error)}")
                        _errorLiveData.value = Event(RefreshBalanceError)
                    }
                )
        }

    private fun getAccountIndexWithError(coin: CoinError): Flowable<Int> {
        logError("Refresh coin balance error: ${coin.error}")
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (account.isAccountToUpdate(coin) && !account.isError) {
                    return accountManager.getCachedCoinBalance(account.address, account.chainId)
                        .map { cachedBalance ->
                            account.cryptoBalance = cachedBalance.balance.cryptoBalance
                            account.fiatBalance = cachedBalance.balance.fiatBalance
                            account.coinRate = cachedBalance.rate ?: Double.InvalidValue
                            account.isError = true
                            index
                        }.toFlowable()
                        .onErrorReturn {
                            account.isError = true
                            index
                        }
                }
            }
        return Flowable.just(Int.InvalidIndex)
    }

    private fun getAccountIndexToUpdate(coinBalance: CoinBalance): Flowable<Int> {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                with(account) {
                    if (shouldUpdateCoinBalance(coinBalance)) {
                        cryptoBalance = coinBalance.balance.cryptoBalance
                        fiatBalance = coinBalance.balance.fiatBalance
                        coinRate = coinBalance.rate ?: Double.InvalidValue
                        isError = false
                        return accountManager.insertCoinBalance(
                            createCoinBalance(
                                account,
                                coinBalance
                            )
                        )
                            .toSingleDefault(index)
                            .onErrorReturnItem(index)
                            .toFlowable()
                    } else if (isAccountToUpdate(coinBalance) && account.isError) {
                        isError = false
                        return Flowable.just(index)
                    }
                }
            }
        return Flowable.just(Int.InvalidIndex)
    }

    private fun createCoinBalance(
        account: Account,
        coinBalance: CoinBalance
    ) = CoinBalance(
        account.chainId,
        account.address,
        Balance(
            coinBalance.balance.cryptoBalance,
            coinBalance.balance.fiatBalance
        ),
        coinBalance.rate ?: Double.InvalidValue
    )

    fun refreshTokensBalances(isAutoDiscoveryRefresh: Boolean = false) {
        streamingDisposable = CompositeDisposable()
        tokenBalancesRefreshed = false
        transactionRepository.getTaggedTokensUpdate()
            .filter { taggedTokens -> taggedTokens.isNotEmpty() }
            .doOnNext {
                launchDisposable {
                    transactionRepository.getTokenBalance()
                        .flatMap { asset -> updateTokenAndGetIndex(asset) }
                        .filter { index -> index != Int.InvalidIndex }
                        .doOnComplete { updateNewTaggedTokens(isAutoDiscoveryRefresh) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onComplete = {
                                newCachedTokens.clear()
                                tokenBalancesRefreshed = true
                                _balanceStateLiveData.value = TokenBalanceCompleted
                            },
                            onNext = { index ->
                                _balanceStateLiveData.value = TokenBalanceUpdate(index)
                            },
                            onError = { error ->
                                logError("Refresh tokens error: ${getError(error)}")
                                _errorLiveData.value = Event(RefreshBalanceError)
                                updateNewTaggedTokens(isAutoDiscoveryRefresh)
                                _balanceStateLiveData.value = TokenBalanceCompleted
                            }
                        )
                }
            }.subscribe()
    }

    private fun updateTokenAndGetIndex(asset: Asset): Flowable<Int> = when (asset) {
        is AssetBalance -> updateTokenAndReturnIndex(asset)
        is AssetError -> updateTokenErrorAndReturnIndex(asset)
        else -> Flowable.just(Int.InvalidIndex)
    }

    private fun updateNewTaggedTokens(isAutoDiscoveryRefresh: Boolean) {
        launchDisposable {
            transactionRepository.updateTaggedTokens()
                .subscribeOn(Schedulers.io())
                .doOnTerminate {
                    if (!isAutoDiscoveryRefresh) {
                        getSuperTokenStreamInitBalance()
                    }
                }
                .subscribeBy(onError = { error -> logger.logToFirebase("Saving tagged tokens error: ${error.message}") })
        }
    }

    private fun getSuperTokenStreamInitBalance() {
        if (transactionRepository.isSuperTokenStreamAvailable) {
            launchDisposable {
                transactionRepository.getSuperTokenStreamInitBalance()
                    .flatMap { asset -> updateTokenAndGetIndex(asset) }
                    .filter { index -> index != Int.InvalidIndex }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { index ->
                            _balanceStateLiveData.value = TokenBalanceUpdate(index)
                        },
                        onError = { error -> logger.logToFirebase("SuperToken init balance error: $error") },
                        onComplete = { handleSuperTokenStreams() }
                    )
            }
        }
    }

    private fun handleSuperTokenStreams() {
        val activeSuperTokenStreams: MutableList<ActiveSuperToken> =
            transactionRepository.activeSuperTokenStreams

        val isActiveSuperTokenVisible: Boolean =
            activeSuperTokenStreams.any { activeSuperToken ->
                tokenVisibilitySettings.getTokenVisibility(
                    activeSuperToken.accountAddress,
                    activeSuperToken.address
                ) ?: false
            }

        if (isActiveSuperTokenVisible) {
            activeSuperTokenStreams
                .distinctBy { activeSuperToken -> activeSuperToken.chainId }
                .forEach { activeSuperToken ->
                    startSuperTokenStreaming(activeSuperToken.chainId)
                }
        }
    }

    private fun startSuperTokenStreaming(chainId: Int) {
        transactionRepository.startSuperTokenStreaming(chainId)
            .flatMap { asset -> updateTokenAndGetIndex(asset) }
            .filter { index -> index != Int.InvalidIndex }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { index -> _balanceStateLiveData.value = TokenBalanceUpdate(index) },
                onError = { error -> logger.logToFirebase("Updating stream error: $error") }
            ).addTo(streamingDisposable)
    }

    private fun updateTokenAndReturnIndex(balance: AssetBalance): Flowable<Int> {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (balance.isTokenInAccount(account)) {
                    return updateTokenVisibilityAndReturnIndex(account, balance, index)
                }
            }
        return Flowable.just(Int.InvalidIndex)
    }

    private fun getError(error: Throwable): String =
        if (error is CompositeException) {
            "${error.exceptions}"
        } else {
            "$error"
        }

    private fun updateNewTokenAndReturnIndex(
        account: Account,
        balance: AssetBalance,
        index: Int
    ): Flowable<Int> {
        if (balance.currentBalance.hasFunds()) {
            saveTokenVisibility(account.address, balance.tokenAddress)
            val cachedAccountToken =
                cachedAccountTokens.findCachedAccountToken(balance.accountToken)
            newTokens.add(getNewAccountToken(balance, cachedAccountToken))
            account.accountTokens = newTokens.filterDistinctAccountTokensForGivenAccount(account)
            return insertTokenBalance(balance, balance.accountAddress)
                .toFlowable<Int>()
                .map { index }
                .onErrorReturnItem(index)
        }
        return Flowable.just(Int.InvalidIndex)
    }

    private fun getNewAccountToken(balance: AssetBalance, cachedAccountToken: AccountToken?) =
        with(balance) {
            if (accountToken.tokenPrice == Double.InvalidValue && cachedAccountToken != null) {
                accountToken.copy(tokenPrice = cachedAccountToken.tokenPrice)
            } else {
                accountToken
            }
        }

    private fun updateTokenErrorAndReturnIndex(balance: AssetError): Flowable<Int> {
        var accountIndex: Int = Int.InvalidIndex
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                if (balance.isTokenInAccount(account)) {
                    if (account.accountTokens.isNotEmpty()) {
                        account.accountTokens.forEach { accountToken ->
                            if (accountToken.isTokenError(balance)) {
                                accountToken.token.isError = true
                                accountIndex = index
                            }
                        }
                    } else {
                        return showCachedTokenBalancesWhenError(balance, account, index)
                    }
                }
            }
        return Flowable.just(accountIndex)
    }

    private fun showCachedTokenBalancesWhenError(
        balance: AssetError,
        account: Account,
        index: Int
    ): Flowable<Int> {
        return accountManager.getCachedTokenBalance(balance.tokenAddress, balance.accountAddress)
            .flatMapPublisher { cachedBalance ->
                cachedTokens[account.chainId]?.findCachedToken(balance)
                    ?.let {
                        newCachedTokens.add(
                            AccountToken(
                                it, currentRawBalance =
                                cachedBalance.balance.cryptoBalance, tokenPrice = cachedBalance.rate
                            )
                        )
                        cachedAccountTokens = newCachedTokens
                            .filter { balance.isTokenInAccount(account) }
                            .toMutableList()
                        Flowable.just(index)
                    }.orElse {
                        Flowable.just(Int.InvalidIndex)
                    }
            }
            .onErrorReturn {
                Int.InvalidIndex
            }
    }

    fun isTokenVisible(accountAddress: String, accountToken: AccountToken): Boolean? =
        tokenVisibilitySettings.getTokenVisibility(accountAddress, accountToken.token.address)

    private fun updateNotVisibleTokenAndReturnIndex(
        account: Account,
        balance: AssetBalance,
        index: Int
    ): Int {
        var accountIndex: Int = Int.InvalidIndex
        if (balance.accountToken.isTokenShown(account)) {
            account.accountTokens.remove(balance.accountToken)
            accountIndex = index
        }
        return accountIndex
    }

    private fun updateTokenVisibilityAndReturnIndex(
        account: Account,
        asset: AssetBalance,
        index: Int
    ): Flowable<Int> {
        var accountIndex: Flowable<Int> = Flowable.just(Int.InvalidIndex)
        isTokenVisible(account.address, asset.accountToken)?.let { isVisible ->
            accountIndex = if (isVisible) {
                updateVisibleTokenAndReturnIndex(account, asset, index)
            } else {
                Flowable.just(updateNotVisibleTokenAndReturnIndex(account, asset, index))
            }
        }.orElse {
            accountIndex = updateNewTokenAndReturnIndex(account, asset, index)
        }
        return accountIndex
    }

    private fun updateVisibleTokenAndReturnIndex(
        account: Account,
        balance: AssetBalance,
        index: Int
    ): Flowable<Int> {
        var accountIndex: Flowable<Int> = Flowable.just(Int.InvalidIndex)
        if (balance.accountToken.isTokenShown(account)) {
            account.accountTokens.find { accountToken ->
                accountToken.isTheSameToken(balance.tokenAddress, balance.accountAddress, balance.accountToken.token.tokenId)
            }?.let { accountToken ->
                if (accountToken.shouldUpdateBalance(balance)) {
                    accountIndex = updateTokenBalanceAndReturnIndex(balance, accountToken, index)
                } else if (accountToken.token.isError) {
                    accountToken.token.isError = false
                    accountIndex = Flowable.just(index)
                }
            }

        } else {
            newTokens.add(getNewAccountToken(balance))
            account.accountTokens = newTokens.filterDistinctAccountTokensForGivenAccount(account)
            return insertTokenBalance(balance, balance.accountAddress)
                .toSingleDefault(index)
                .map { id ->
                    if (balance.accountToken.token.isStreamActive) {
                        updateInitSuperTokenStreamBalance(balance, balance.accountToken)
                    }
                    id
                }
                .onErrorReturnItem(index)
                .toFlowable()
        }
        return accountIndex
    }

    private fun getNewAccountToken(balance: AssetBalance): AccountToken =
        if (balance.accountToken.tokenPrice == Double.InvalidValue) {
            val cachedAccountToken =
                cachedAccountTokens.findCachedAccountToken(balance.accountToken)
            balance.accountToken.copy(tokenPrice = cachedAccountToken?.tokenPrice)
        } else {
            balance.accountToken
        }

    private fun updateSuperTokenStreamAndReturnIndex(
        accountToken: AccountToken,
        balance: AssetBalance,
        index: Int
    ): Int = with(accountToken) {
        when {
            nextBalance == Double.InvalidValue.toBigDecimal() -> {
                updateInitSuperTokenStreamBalance(balance, accountToken)
                index
            }
            nextBalance != balance.accountToken.nextBalance -> {
                updateNextSuperTokenStreamBalance(balance, accountToken)
                index
            }
            else -> Int.InvalidIndex
        }
    }

    private fun updateNextSuperTokenStreamBalance(
        balance: AssetBalance,
        accountToken: AccountToken
    ) = with(accountToken) {
        currentRawBalance = nextRawBalance
        nextRawBalance = balance.accountToken.nextRawBalance
        token.isStreamActive = balance.accountToken.token.isStreamActive
        isInitStream = false
        token.isError = false
        token.consNetFlow = balance.accountToken.token.consNetFlow
    }

    private fun updateInitSuperTokenStreamBalance(
        balance: AssetBalance,
        accountToken: AccountToken
    ) = with(accountToken) {
        currentRawBalance = balance.accountToken.currentRawBalance
        nextRawBalance = balance.accountToken.nextRawBalance
        isInitStream = true
        token.isError = false
        token.consNetFlow = balance.accountToken.token.consNetFlow
    }

    private fun updateTokenBalanceAndReturnIndex(
        balance: AssetBalance,
        accountToken: AccountToken,
        index: Int
    ): Flowable<Int> =
        if (balance.accountToken.token.isStreamActive) {
            Flowable.just(updateSuperTokenStreamAndReturnIndex(accountToken, balance, index))
        } else {
            with(accountToken) {
                currentRawBalance = balance.accountToken.currentRawBalance
                setNewTokenPrice(balance, cachedAccountTokens)
                token.isError = false
                insertTokenBalance(balance, accountToken.token.accountAddress)
                    .toSingleDefault(index)
                    .onErrorReturnItem(index)
                    .toFlowable()
            }
        }

    private fun insertTokenBalance(balance: AssetBalance, accountAddress: String): Completable =
        accountManager.insertTokenBalance(
            coinBalance(balance, balance.accountToken),
            accountAddress
        )

    private fun coinBalance(balance: AssetBalance, accountToken: AccountToken): CoinBalance =
        CoinBalance(
            chainId = accountToken.token.chainId,
            address = accountToken.token.address,
            balance = Balance(balance.accountToken.currentRawBalance),
            rate = accountToken.tokenPrice
        )

    fun stopStreaming() {
        transactionRepository.disconnectFromSuperTokenStreaming()
        if (::streamingDisposable.isInitialized) {
            streamingDisposable.dispose()
        }
        resetStreamingAnimation()
    }

    private fun resetStreamingAnimation() {
        activeAccounts.forEach { account ->
            account.accountTokens.forEach { accountToken ->
                accountToken.nextRawBalance = Double.InvalidValue.toBigDecimal()
                accountToken.isInitStream = true
            }
        }
    }

    fun updateTokensRate() {
        launchDisposable {
            transactionRepository.getTokensRates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onComplete = {
                    transactionRepository.updateTokensRate()
                    _balanceStateLiveData.value = UpdateAllState
                })
        }
    }

    fun discoverNewTokens() =
        launchDisposable {
            transactionRepository.discoverNewTokens()
                .filter { shouldRefreshTokensBalances -> shouldRefreshTokensBalances }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        fetchNFTData()
                        refreshTokensBalances(true)
                    },
                    onError = { error -> logError("Error while token auto-discovery: $error") }
                )
        }

    private fun logError(message: String) {
        logger.logToFirebase(message)
        Timber.e(message)
    }

    private fun handleHideAccountErrors(error: Throwable) {
        logError("Error while hiding account: $error")
        _errorLiveData.value = when (error) {
            is BalanceIsNotEmptyThrowable -> Event(BalanceIsNotEmptyError)
            is BalanceIsNotEmptyAndHasMoreOwnersThrowable -> Event(
                BalanceIsNotEmptyAndHasMoreOwnersError
            )
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
                safeAccountRepository.createSafeAccount(account)
                    .flatMapCompletable { smartContractAddress ->
                        accountManager.createSafeAccount(account, smartContractAddress)
                    }
                    .observeOn(Schedulers.io())
                    .andThen(
                        walletActionsRepository.saveWalletActions(
                            getSafeAccountWalletAction(
                                account
                            )
                        )
                    )
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

    private fun saveTokenVisibility(networkAddress: String, tokenAddress: String) {
        tokenVisibilitySettings = accountManager.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(networkAddress, tokenAddress, true)
        )
    }

    fun createNewAccount(chainId: Int) {
        launchDisposable {
            accountManager.createOrUnhideAccount(NetworkManager.getNetwork(chainId))
                .flatMapCompletable {
                    walletActionsRepository.saveWalletActions(
                        listOf(
                            getWalletAction(
                                WalletActionStatus.ADDED,
                                it
                            )
                        )
                    )
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

    /**
     * Change Show Warning - change value for "showWarning" property (Account::showWarning)
     * @param existedAccount - instance of minerva.android.walletmanager.model.minervaprimitives.account.Accont
     *     item which value will be changed
     * @param state - new state for Account::showWarning
     */
    fun changeShowWarning(account: Account, state: Boolean) {
        launchDisposable {
            accountManager.changeShowWarning(account, state)
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

    fun updateSessionCount(
        sessionsPerAccount: List<DappSessionData>,
        passIndex: (index: Int) -> Unit
    ) {
        activeAccounts
            .filter { account -> !account.isPending }
            .forEachIndexed { index, account ->
                account.apply {
                    dappSessionCount = sessionsPerAccount
                        .find { dappSession ->
                            dappSession.address.equals(
                                address,
                                true
                            ) && dappSession.chainId == chainId
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

    private fun shouldShowPending(
        account: Account,
        index: Int,
        chainId: Int,
        areMainNetsEnabled: Boolean
    ): Boolean =
        account.id == index && account.chainId == chainId && account.network.testNet != areMainNetsEnabled

    companion object {
        private const val DAY: Long = 24L
        private const val NO_DAPP_SESSION = 0
    }
}
