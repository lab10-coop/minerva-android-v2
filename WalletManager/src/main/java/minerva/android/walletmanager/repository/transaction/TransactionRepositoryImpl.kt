package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.GasPricesMatic
import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.TransactionSpeed
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.mappers.PendingTransactionToPendingAccountMapper
import minerva.android.walletmanager.model.mappers.TransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.TransactionToTransactionPayloadMapper
import minerva.android.walletmanager.model.mappers.TxCostPayloadToTxCostDataMapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.*
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

    override val masterSeed: MasterSeed
        get() = walletConfigManager.masterSeed

    override fun refreshCoinBalances(): Single<HashMap<String, Balance>> =
        walletConfigManager.getWalletConfig().accounts.filter { account -> accountsFilter(account)  && !account.isEmptyAccount }.let { accounts ->
            blockchainRepository.refreshBalances(getAddresses(accounts))
                .zipWith(getRate(MarketUtils.getMarketsIds(accounts)).onErrorReturnItem(Markets()))
                .map { (cryptoBalances, markets) ->
                    MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets, localStorage.loadCurrentFiat())
                }
        }

    private fun getAddresses(accounts: List<Account>): List<Pair<Int, String>> =
        accounts.map { account -> account.network.chainId to account.address }

    override fun refreshTokensBalances(): Single<Map<String, List<AccountToken>>> =
        accountsForTokenBalanceRefresh.let { accounts ->
            Observable.fromIterable(accounts)
                .flatMapSingle { account -> tokenManager.refreshTokensBalances(account) }
                .toList()
                .map { accountTokensPerAccountList -> parseAccountTokensPerAccountListToMap(accountTokensPerAccountList) }
                .flatMap { accountTokensPerAccountMap -> handleNewAddedTokens(accountTokensPerAccountMap, accounts) }
        }

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

    private fun handleNewAddedTokens(
        accountTokensPerAccountMap: Map<String, List<AccountToken>>,
        accounts: List<Account>
    ): Single<Map<String, List<AccountToken>>> =
        if (shouldSetAccountAddress(accountTokensPerAccountMap)) {
            updateCachedTokens(getTokensWithAccountAddress(accounts, accountTokensPerAccountMap))
                .toSingleDefault(accountTokensPerAccountMap)
                .onErrorResumeNext { Single.just(accountTokensPerAccountMap) }
            //TODO what should be done when error happens? should crash app to prevent losing data?
        } else {
            Single.just(accountTokensPerAccountMap)
        }

    internal fun getTokensWithAccountAddress(
        accounts: List<Account>,
        accountTokensPerAccountMap: Map<String, List<AccountToken>>
    ): Map<Int, List<ERC20Token>> {
        val allTokens: MutableList<ERC20Token> = mutableListOf()
        accounts.forEach { account ->
            accountTokensPerAccountMap[account.privateKey]?.forEach { accountToken ->
                if (accountToken.balance > BigDecimal.ZERO) {
                    allTokens.add(accountToken.token.copy(accountAddress = account.address, tag = accountToken.token.tag))
                }
            }
        }
        return allTokens.groupBy { token -> token.chainId }
    }

    private fun shouldSetAccountAddress(accountTokensPerAccountMap: Map<String, List<AccountToken>>): Boolean =
        accountTokensPerAccountMap.values.any { accountTokens ->
            accountTokens.find { accountToken -> accountToken.balance > BigDecimal.ZERO && accountToken.token.accountAddress.isBlank() } != null
        }

    private fun parseAccountTokensPerAccountListToMap(accountTokenPerAccountList: List<Pair<String, List<AccountToken>>>) =
        mutableMapOf<String, List<AccountToken>>()
            .apply {
                accountTokenPerAccountList.forEach { (privateKey, accountTokens) -> put(privateKey, accountTokens) }
            }.toMap()

    private fun updateCachedTokens(updatedTokens: Map<Int, List<ERC20Token>>): Completable =
        with(walletConfigManager.getWalletConfig()) {
            walletConfigManager.updateWalletConfig(copy(version = updateVersion, erc20Tokens = updatedTokens))
        }

    override fun getTokensRates(): Completable =
        walletConfigManager.getWalletConfig().let { config -> tokenManager.getTokensRates(config.erc20Tokens) }

    override fun updateTokensRate() {
        getActiveAccounts().forEach { tokenManager.updateTokensRate(it) }
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
                                .flatMap { (shouldBeUpdated, newAndLocalTokensPerChainIdMap) ->
                                    tokenManager.updateTokenIcons(shouldBeUpdated, newAndLocalTokensPerChainIdMap)
                                        .onErrorReturn {
                                            Timber.e(it)
                                            Pair(false, newAndLocalTokensPerChainIdMap)
                                        }
                                }
                                .flatMap { (shouldBeSaved, newAndLocalTokensPerChainIdMap) ->
                                    tokenManager.saveTokens(shouldBeSaved, newAndLocalTokensPerChainIdMap)
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
        localStorage.loadCurrentFiat().let { currentFiat ->
            when (chainId) {
                ChainId.ETH_MAIN -> getRate(MarketIds.ETHEREUM).map { it.ethFiatPrice?.getRate(currentFiat) }
                ChainId.POA_CORE -> getRate(MarketIds.POA_NETWORK).map { it.poaFiatPrice?.getRate(currentFiat) }
                ChainId.XDAI -> getRate(MarketIds.XDAI).map { it.daiFiatPrice?.getRate(currentFiat) }
                ChainId.MATIC -> getRate(MarketIds.MATIC).map { it.maticFiatPrice?.getRate(currentFiat) }
                else -> Single.just(ZERO_FIAT_VALUE)
            }
        }

    override fun getTokenFiatRate(tokenHash: String): Single<Double> =
        Single.just(tokenManager.getSingleTokenRate(tokenHash))

    private fun getRate(id: String): Single<Markets> =
        cryptoApi.getMarkets(id, localStorage.loadCurrentFiat().toLowerCase(Locale.ROOT))

    override fun toUserReadableFormat(value: BigDecimal): BigDecimal = blockchainRepository.toEther(value)

    override fun sendTransaction(chainId: Int, transaction: Transaction): Single<String> =
        blockchainRepository.sendWalletConnectTransaction(chainId, TransactionToTransactionPayloadMapper.map(transaction))

    override fun getFiatSymbol(): String = Fiat.getFiatSymbol(localStorage.loadCurrentFiat())

    override fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost> = with(txCostPayload) {
        when {
            shouldGetGasPriceFromApi(chainId) && isMaticNetwork(chainId) -> {
                cryptoApi.getGasPriceForMatic(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                    .flatMap { gasPricesMatic -> getTxCosts(txCostPayload, gasPricesMatic.toTransactionSpeed()) }
                    .onErrorResumeNext { getTxCosts(txCostPayload, null) }
            }
            shouldGetGasPriceFromApi(chainId) -> {
                cryptoApi.getGasPrice(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                    .flatMap { gasPrice -> getTxCosts(txCostPayload, gasPrice.speed) }
                    .onErrorResumeNext { getTxCosts(txCostPayload, null) }
            }
            else -> getTxCosts(txCostPayload, null)
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
    override fun getAccountByAddress(address: String): Account? =
        walletConfigManager.getWalletConfig().accounts.find { it.address.equals(address, true) }

    override fun getFreeATS(address: String) = blockchainRepository.getFreeATS(address)
    override fun checkMissingTokensDetails(): Completable = tokenManager.checkMissingTokensDetails()
    override fun isProtectTransactionEnabled(): Boolean = localStorage.isProtectTransactionsEnabled
    private fun shouldGetGasPriceFromApi(chainId: Int) = NetworkManager.getNetwork(chainId).gasPriceOracle.isNotEmpty()

    private fun isMaticNetwork(chainId: Int) = chainId == MATIC || chainId == MUMBAI

    private fun getTxCosts(payload: TxCostPayload, speed: TransactionSpeed?): Single<TransactionCost> =
        blockchainRepository.getTransactionCosts(TxCostPayloadToTxCostDataMapper.map(payload), speed?.rapid)
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