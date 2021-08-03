package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.GasPricesMatic
import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.TransactionSpeed
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit

class TransactionRepositoryImpl(
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val walletConfigManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val webSocketRepository: WebSocketRepository,
    private val tokenManager: TokenManager
) : TransactionRepository {
    override val masterSeed: MasterSeed get() = walletConfigManager.masterSeed
    override var newTaggedTokens: MutableList<ERC20Token> = mutableListOf()
    private val currentFiatCurrency: String get() = localStorage.loadCurrentFiat()
    private val ratesMap: HashMap<Int, Markets> = hashMapOf()

    override fun getCoinBalance(): Flowable<Coin> =
        walletConfigManager.getWalletConfig().accounts
            .filter { account -> accountsFilter(account) && !account.isEmptyAccount }
            .let { accounts ->
                ratesMap.clear()
                blockchainRepository.getCoinBalances(getAddresses(accounts))
                    .flatMapSingle { token ->
                        when (token) {
                            is TokenWithBalance -> getCoinRates(TokenToCoinCryptoBalanceMapper.map(token))
                            else -> Single.just(TokenToCoinBalanceErrorMapper.map(token as TokenWithError))
                        }
                    }
            }

    private fun getCoinRates(cryptoBalance: CoinCryptoBalance): Single<CoinBalance> = with(cryptoBalance) {
        val marketId = MarketUtils.getCoinGeckoMarketId(chainId)
        return if (marketId != String.Empty && balance > BigDecimal.ZERO) {
            ratesMap[chainId]?.let { markets ->
                getStoredRate(markets, cryptoBalance, marketId)
            }.orElse {
                getMarkets(marketId, ratesMap, cryptoBalance)
            }
        } else {
            Single.just(calculateFiat(cryptoBalance))
        }
    }

    private fun getStoredRate(
        markets: Markets,
        cryptoBalance: CoinCryptoBalance,
        marketId: String
    ): Single<CoinBalance> =
        if (MarketUtils.getRate(cryptoBalance.chainId, markets, currentFiatCurrency) != Double.InvalidValue) {
            Single.just(calculateFiat(cryptoBalance, markets))
        } else {
            getMarkets(marketId, ratesMap, cryptoBalance)
        }

    private fun getMarkets(
        marketId: String,
        ratesMap: HashMap<Int, Markets>,
        cryptoBalance: CoinCryptoBalance
    ): Single<CoinBalance> =
        cryptoApi.getMarkets(marketId, currentFiatCurrency.toLowerCase(Locale.ROOT))
            .onErrorReturnItem(Markets())
            .map { market ->
                ratesMap[cryptoBalance.chainId] = market
                calculateFiat(cryptoBalance, market)
            }

    private fun calculateFiat(cryptoCoinBalance: CoinCryptoBalance, market: Markets = Markets()): CoinBalance =
        MarketUtils.calculateFiatBalance(cryptoCoinBalance, market, currentFiatCurrency)

    private fun fetchCoinRate(id: String): Single<Markets> =
        cryptoApi.getMarkets(id, currentFiatCurrency.toLowerCase(Locale.ROOT))

    private fun getAddresses(accounts: List<Account>): List<Pair<Int, String>> =
        accounts.map { account -> account.network.chainId to account.address }

    override fun getTokenBalance(): Flowable<Asset> =
        accountsForTokenBalanceRefresh.let { accounts ->
            Flowable.mergeDelayError(getTokenBalanceFlowables(accounts))
                .flatMap { asset ->
                    when (asset) {
                        is AssetBalance -> handleNewAddedTokens(asset, accounts)
                        else -> Flowable.just(asset as AssetError)
                    }
                }
        }

    private fun getTokenBalanceFlowables(accounts: List<Account>): List<Flowable<Asset>> =
        mutableListOf<Flowable<Asset>>().apply {
            accounts.forEach { account -> add(tokenManager.getTokenBalance(account).subscribeOn(Schedulers.io())) }
        }

    private fun handleNewAddedTokens(assetBalance: AssetBalance, accounts: List<Account>): Flowable<AssetBalance> =
        if (isAccountAddressMissing(assetBalance)) {
            accounts.forEach { account ->
                if (isTokenWithPositiveBalance(account, assetBalance)) {
                    newTaggedTokens.add(
                        assetBalance.accountToken.token.copy(
                            accountAddress = account.address,
                            tag = assetBalance.accountToken.token.tag
                        )
                    )
                }
            }
            Flowable.just(assetBalance)
        } else {
            Flowable.just(assetBalance)
        }

    private fun isAccountAddressMissing(assetBalance: AssetBalance): Boolean =
        assetBalance.accountToken.balance > BigDecimal.ZERO && assetBalance.accountToken.token.accountAddress.isBlank()

    override fun updateCachedTokens(): Completable {
        return if (newTaggedTokens.isNotEmpty()) {
            val walletConfig = walletConfigManager.getWalletConfig()
            return walletConfigManager.updateWalletConfig(
                walletConfig.copy(version = walletConfig.updateVersion, erc20Tokens = getTokensWithAccountAddress())
            ).doOnComplete { newTaggedTokens.clear() }
        } else {
            Completable.complete()
        }
    }

    internal fun getTokensWithAccountAddress(): Map<Int, List<ERC20Token>> {
        accountsForTokenBalanceRefresh.let { accounts ->
            val allTokens: MutableList<ERC20Token> = mutableListOf()
            accounts.forEach { account ->
                newTaggedTokens.forEach { token ->
                    if (account.address.equals(token.accountAddress, true) && account.chainId == token.chainId) {
                        allTokens.add(token.copy(accountAddress = account.address, tag = token.tag))
                    }
                }
            }
            return allTokens.groupBy { token -> token.chainId }
        }
    }

    private fun isTokenWithPositiveBalance(account: Account, assetBalance: AssetBalance): Boolean =
        account.privateKey.equals(assetBalance.privateKey, true) &&
                account.chainId == assetBalance.chainId && assetBalance.accountToken.balance > BigDecimal.ZERO

    private val accountsForTokenBalanceRefresh: List<Account>
        get() = if (shouldGetAllAccounts()) {
            getActiveAccountsOnTestAndMainNets()
        } else {
            getActiveAccounts()
        }

    private fun getActiveAccountsOnTestAndMainNets() =
        walletConfigManager.getWalletConfig().accounts
            .filter { account -> refreshBalanceFilter(account) && account.network.isAvailable() }

    private fun shouldGetAllAccounts() =
        walletConfigManager.getWalletConfig().erc20Tokens.values
            .any { tokens -> tokens.find { token -> token.accountAddress.isBlank() } != null }

    private fun getActiveAccounts(): List<Account> =
        walletConfigManager.getWalletConfig()
            .accounts.filter { account -> accountsFilter(account) && account.network.isAvailable() }

    private fun accountsFilter(account: Account) =
        refreshBalanceFilter(account) && account.network.testNet == !localStorage.areMainNetworksEnabled

    private fun refreshBalanceFilter(account: Account) = !account.isHide && !account.isDeleted && !account.isPending
    override fun getTaggedTokensUpdate(): Flowable<List<ERC20Token>> = tokenManager.getTaggedTokensUpdate()

    override fun getTokensRates(): Completable =
        walletConfigManager.getWalletConfig().let { config -> tokenManager.getTokensRates(config.erc20Tokens) }

    override fun updateTokensRate() {
        getActiveAccounts().forEach { account -> tokenManager.updateTokensRate(account) }
    }

    override fun discoverNewTokens(): Single<Boolean> =
        getActiveAccounts().let { accounts ->
            accounts.filter { account -> NetworkManager.isUsingEtherScan(account.chainId) }
                .let { etherscanAccounts ->
                    accounts.filter { account -> !NetworkManager.isUsingEtherScan(account.chainId) }
                        .let { notEtherscanAccounts ->
                            downloadTokensListWithBuffer(etherscanAccounts)
                                .zipWith(downloadTokensList(notEtherscanAccounts))
                                .map { (etherscanTokens, notEtherscanTokens) -> etherscanTokens + notEtherscanTokens }
                                .map { newTokens -> tokenManager.sortTokensByChainId(newTokens) }
                                .map { newTokensPerChainIdMap ->
                                    tokenManager.mergeWithLocalTokensList(newTokensPerChainIdMap)
                                }
                                .flatMap { (shouldUpdateIcon, newAndLocalTokensPerChainIdMap) ->
                                    tokenManager.updateTokenIcons(shouldUpdateIcon, newAndLocalTokensPerChainIdMap)
                                        .onErrorReturn {
                                            Timber.e(it)
                                            Pair(false, newAndLocalTokensPerChainIdMap)
                                        }
                                }
                                .flatMap { (shouldSafeNewTokens, newAndLocalTokensPerChainIdMap) ->
                                    tokenManager.saveTokens(shouldSafeNewTokens, newAndLocalTokensPerChainIdMap)
                                        .onErrorReturn {
                                            Timber.e(it)
                                            false
                                        }
                                }
                        }
                }
        }

    private fun downloadTokensListWithBuffer(accounts: List<Account>): Single<List<ERC20Token>> =
        Observable.fromIterable(accounts)
            .buffer(ETHERSCAN_REQUEST_TIMESPAN, TimeUnit.SECONDS, ETHERSCAN_REQUEST_PACKAGE)
            .flatMapSingle { accountList -> downloadTokensList(accountList) }
            .toList()
            .map { tokens -> mergeLists(tokens) }

    private fun downloadTokensList(accounts: List<Account>): Single<List<ERC20Token>> =
        Observable.fromIterable(accounts)
            .flatMapSingle { account -> tokenManager.downloadTokensList(account) }
            .toList()
            .map { tokens -> mergeLists(tokens) }

    private fun mergeLists(lists: List<List<ERC20Token>>): List<ERC20Token> =
        mutableListOf<ERC20Token>().apply {
            lists.forEach { tokens -> addAll(tokens) }
        }

    override fun transferNativeCoin(chainId: Int, accountIndex: Int, transaction: Transaction): Completable =
        blockchainRepository.transferNativeCoin(
            chainId,
            accountIndex,
            TransactionToTransactionPayloadMapper.map(transaction)
        ).map { pendingTx ->
            /*Subscription to web sockets doesn't work with http rpc, hence pending tsx are not saved*/
            if (NetworkManager.getNetwork(pendingTx.chainId).wsRpc.isNotEmpty()) {
                localStorage.savePendingAccount(PendingTransactionToPendingAccountMapper.map(pendingTx))
            }
        }.flatMap { blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty } }
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun getTransactions(): Single<List<PendingAccount>> =
        blockchainRepository.getTransactions(getTxHashes())
            .map { pendingTxs -> getPendingAccountsWithBlockHashes(pendingTxs) }

    override fun getCoinFiatRate(chainId: Int): Single<Double> =
        currentFiatCurrency.let { currentFiat ->
            when (chainId) {
                ChainId.ETH_MAIN -> fetchCoinRate(MarketIds.ETHEREUM).map { it.ethFiatPrice?.getRate(currentFiat) }
                ChainId.POA_CORE -> fetchCoinRate(MarketIds.POA_NETWORK).map { it.poaFiatPrice?.getRate(currentFiat) }
                ChainId.XDAI -> fetchCoinRate(MarketIds.XDAI).map { it.daiFiatPrice?.getRate(currentFiat) }
                ChainId.MATIC -> fetchCoinRate(MarketIds.MATIC).map { it.maticFiatPrice?.getRate(currentFiat) }
                else -> Single.just(ZERO_FIAT_VALUE)
            }
        }

    override fun getTokenFiatRate(tokenHash: String): Single<Double> =
        Single.just(tokenManager.getSingleTokenRate(tokenHash))

    override fun toUserReadableFormat(value: BigDecimal): BigDecimal = blockchainRepository.toEther(value)

    override fun sendTransaction(chainId: Int, transaction: Transaction): Single<String> =
        blockchainRepository.sendWalletConnectTransaction(chainId, TransactionToTransactionPayloadMapper.map(transaction))

    override fun getFiatSymbol(): String = Fiat.getFiatSymbol(currentFiatCurrency)

    override fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost> = with(txCostPayload) {
        when {
            shouldGetGasPriceFromApi(chainId) && isMaticNetwork(chainId) -> {
                cryptoApi.getGasPriceForMatic(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                    .flatMap { gasPricesMatic ->
                        getTxCosts(
                            txCostPayload,
                            gasPricesMatic.toTransactionSpeed(),
                            gasPricesMatic.toTransactionSpeed().standard
                        )
                    }
                    .onErrorResumeNext { getTxCosts(txCostPayload, null, null) }
            }
            shouldGetGasPriceFromApi(chainId) -> {
                cryptoApi.getGasPrice(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                    .flatMap { gasPrice -> getTxCosts(txCostPayload, gasPrice.speed, gasPrice.speed.fast) }
                    .onErrorResumeNext { getTxCosts(txCostPayload, null, null) }
            }
            else -> getTxCosts(txCostPayload, null, null)
        }
    }

    override fun isAddressValid(address: String): Boolean = blockchainRepository.isAddressValid(address)

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.run { getTransactionCostInEth(toGwei(gasPrice), BigDecimal(gasLimit)) }

    override fun shouldOpenNewWssConnection(accountIndex: Int): Boolean {
        val chainId = getPendingAccount(accountIndex).chainId
        return when {
            getPendingAccounts().size == ONE_PENDING_ACCOUNT -> isFirstAccountPending(accountIndex, chainId)
            getPendingAccounts().size > ONE_PENDING_ACCOUNT -> isNetworkAlreadyPending(chainId)
            else -> false
        }
    }

    override fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount> {
        val pendingAccount = getPendingAccount(accountIndex)
        return webSocketRepository.subscribeToExecutedTransactions(pendingAccount.chainId, pendingAccount.blockNumber)
            .filter { findPendingAccount(it) != null }
            .map { findPendingAccount(it) }
    }

    override fun removePendingAccount(pendingAccount: PendingAccount) {
        localStorage.removePendingAccount(pendingAccount)
    }

    override fun getPendingAccount(accountIndex: Int): PendingAccount =
        localStorage.getPendingAccounts().find { it.index == accountIndex }.orElse { PendingAccount(Int.InvalidIndex) }

    override fun clearPendingAccounts() {
        localStorage.clearPendingAccounts()
    }

    override fun getPendingAccounts(): List<PendingAccount> = localStorage.getPendingAccounts()
    override fun transferERC20Token(chainId: Int, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(chainId, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun loadRecipients(): List<Recipient> = localStorage.getRecipients()
    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)
    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)
    override fun getAccountByAddressAndChainId(address: String, chainId: Int): Account? =
        walletConfigManager.getWalletConfig().accounts.find { account ->
            account.address.equals(address, true) && account.chainId == chainId
        }

    override fun getFreeATS(address: String) = blockchainRepository.getFreeATS(address)
    override fun checkMissingTokensDetails(): Completable = tokenManager.checkMissingTokensDetails()
    override fun isProtectTransactionEnabled(): Boolean = localStorage.isProtectTransactionsEnabled
    private fun shouldGetGasPriceFromApi(chainId: Int) = NetworkManager.getNetwork(chainId).gasPriceOracle.isNotEmpty()

    private fun isMaticNetwork(chainId: Int) = chainId == MATIC || chainId == MUMBAI

    private fun getTxCosts(
        payload: TxCostPayload,
        speed: TransactionSpeed?,
        defaultGasPrice: BigDecimal?
    ): Single<TransactionCost> =
        blockchainRepository.getTransactionCosts(TxCostPayloadToTxCostDataMapper.map(payload), defaultGasPrice)
            .map { txCost ->
                TransactionCostPayloadToTransactionCost.map(txCost, speed, payload.chainId) {
                    blockchainRepository.fromWei(it).setScale(0, RoundingMode.HALF_EVEN)
                }
            }

    private fun GasPricesMatic.toTransactionSpeed() = TransactionSpeed(
        rapid = blockchainRepository.toGwei(rapid),
        fast = blockchainRepository.toGwei(fast),
        standard = blockchainRepository.toGwei(standard),
        slow = blockchainRepository.toGwei(slow)
    )

    private fun getPendingAccountsWithBlockHashes(pendingTxList: List<Pair<String, String?>>): MutableList<PendingAccount> {
        val pendingList = mutableListOf<PendingAccount>()
        pendingTxList.forEach { (transactionHash, blockHash) ->
            localStorage.getPendingAccounts().forEach { tx ->
                if (transactionHash == tx.txHash) {
                    tx.blockHash = blockHash
                    pendingList.add(tx)
                }
            }
        }
        return pendingList
    }

    private fun getTxHashes(): MutableList<Pair<Int, String>> {
        val txHashes = mutableListOf<Pair<Int, String>>()
        localStorage.getPendingAccounts().forEach {
            txHashes.add(Pair(it.chainId, it.txHash))
        }
        return txHashes
    }

    private fun isFirstAccountPending(accountIndex: Int, chainId: Int) =
        getPendingAccounts().find { it.index == accountIndex && it.chainId == chainId } != null

    private fun isNetworkAlreadyPending(chainId: Int) =
        getPendingAccounts().filter { it.chainId == chainId }.size < PENDING_NETWORK_LIMIT &&
                getPendingAccounts().first().chainId != chainId

    private fun findPendingAccount(transaction: ExecutedTransaction): PendingAccount? =
        localStorage.getPendingAccounts()
            .find { pendingAccount -> pendingAccount.txHash == transaction.txHash && pendingAccount.senderAddress == transaction.senderAddress }

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val PENDING_NETWORK_LIMIT = 2
        private const val ETHERSCAN_REQUEST_TIMESPAN = 1L
        private const val ETHERSCAN_REQUEST_PACKAGE = 5
        private const val ZERO_FIAT_VALUE = 0.0
    }
}