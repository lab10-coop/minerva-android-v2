package minerva.android.walletmanager.repository.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.TransactionSpeed
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.transaction.BlockchainTransactionRepository
import minerva.android.blockchainprovider.repository.units.UnitConverter
import minerva.android.blockchainprovider.repository.validation.ValidationRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_ONE
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_C
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.defs.ChainId.Companion.POLYGON
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZKS_ERA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZK_EVM
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.repository.asset.AssetBalanceRepository
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.ConnectException
import java.util.*
import java.util.concurrent.TimeUnit

class TransactionRepositoryImpl(
    private val blockchainRepository: BlockchainTransactionRepository,
    private val walletConfigManager: WalletConfigManager,
    private val erC20TokenRepository: ERC20TokenRepository,
    private val erc721TokenRepository: ERC721TokenRepository,
    private val erc1155TokenRepository: ERC1155TokenRepository,
    private val unitConverter: UnitConverter,
    private val ensRepository: ENSRepository,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val webSocketRepository: WebSocketRepository,
    private val tokenManager: TokenManager,
    private val validationRepository: ValidationRepository,
    private val assetBalanceRepository: AssetBalanceRepository
) : TransactionRepository {
    override val masterSeed: MasterSeed get() = walletConfigManager.masterSeed
    override var newTokens: MutableList<ERCToken> = mutableListOf()
    override val assetBalances: MutableList<AssetBalance> get() = assetBalanceRepository.assetBalances
    private val currentFiatCurrency: String get() = localStorage.loadCurrentFiat()
    private val ratesMap: HashMap<Int, Markets> = hashMapOf()

    private val _ratesMapLiveData = MutableLiveData<Event<Unit>>()
    override val ratesMapLiveData : LiveData<Event<Unit>> get() = _ratesMapLiveData

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

    override fun getCoinBalanceByAccount(account: Account): Flowable<Coin> =
        blockchainRepository.getCoinBalances(listOf(account.network.chainId to account.address))
            .flatMapSingle { token ->
                when (token) {
                    is TokenWithBalance -> getCoinRates(TokenToCoinCryptoBalanceMapper.map(token))
                    else -> Single.just(TokenToCoinBalanceErrorMapper.map(token as TokenWithError))
                }
            }

    private fun getCoinRates(cryptoBalance: CoinCryptoBalance): Single<CoinBalance> = with(cryptoBalance) {
        val marketId = MarketUtils.getCoinGeckoMarketId(chainId)
        return if (marketId != String.Empty && balance > BigDecimal.ZERO) {
            ratesMap[chainId]?.let { markets ->
                getStoredRate(markets, cryptoBalance, marketId)
            }.orElse {
                getMarkets(marketId, cryptoBalance)
            }
        } else {
            Single.just(calculateFiat(cryptoBalance))
        }
    }

    private fun calculateFiat(cryptoCoinBalance: CoinCryptoBalance, market: Markets = Markets()): CoinBalance =
        MarketUtils.calculateFiatBalance(cryptoCoinBalance, market, currentFiatCurrency)

    private fun getStoredRate(markets: Markets, cryptoBalance: CoinCryptoBalance, marketId: String): Single<CoinBalance> =
        if (MarketUtils.getRate(cryptoBalance.chainId, markets, currentFiatCurrency) != Double.InvalidValue) {
            Single.just(calculateFiat(cryptoBalance, markets))
        } else {
            getMarkets(marketId,  cryptoBalance)
        }

    private fun getMarkets(
        marketId: String,
        cryptoBalance: CoinCryptoBalance
    ): Single<CoinBalance> =
        cryptoApi.getMarkets(marketId, currentFiatCurrency.lowercase(Locale.ROOT))
            .onErrorReturnItem(Markets())
            .map { market ->
                ratesMap[cryptoBalance.chainId] = market
                calculateFiat(cryptoBalance, market)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                _ratesMapLiveData.value = Event(Unit)
            }

    private fun fetchCoinRate(id: String): Single<Markets> =
        cryptoApi.getMarkets(id, currentFiatCurrency.lowercase(Locale.ROOT))

    private fun getAddresses(accounts: List<Account>): List<Pair<Int, String>> =
        accounts.map { account -> account.network.chainId to account.address }

    override fun getSuperTokenStreamInitBalance(): Flowable<Asset> {
        accountsForTokenBalanceRefresh.let {
            val accountsWithActiveStreams: List<Account> = getActiveAccountsWithSuperfluidSupport()
            return Flowable.mergeDelayError(getSuperTokenBalanceFlowables(accountsWithActiveStreams))
                .flatMap { asset -> handleSuperTokens(asset) }
        }
    }

    @Throws(ConnectException::class)
    override fun startSuperTokenStreaming(chainId: Int): Flowable<Asset> {
        accountsForTokenBalanceRefresh.let {
            val accountsWithActiveStreams: List<Account> = getActiveAccountsWithSuperfluidSupport()
                    .filter { account -> account.chainId == chainId }
            return Flowable.mergeDelayError(getSuperTokenBalanceFlowables(accountsWithActiveStreams))
                .flatMap { asset -> handleSuperTokens(asset) }
        }
    }

    private fun handleSuperTokens(asset: Asset) =
        when (asset) {
            is AssetBalance -> Flowable.just(asset)
                .filter { assetBalance -> assetBalance.accountToken.token.type.isSuperToken() }
            else -> Flowable.just(asset as AssetError)
        }


    private fun getSuperTokenBalanceFlowables(accounts: List<Account>): List<Flowable<Asset>> =
        mutableListOf<Flowable<Asset>>().apply {
            accounts.forEach { account -> add(tokenManager.getSuperTokenBalance(account).subscribeOn(Schedulers.io())) }
        }

    override fun getTokenBalance(): Flowable<Asset> =
        accountsForTokenBalanceRefresh.let { accounts ->
            assetBalances.clear()
            Flowable.mergeDelayError(getTokenBalanceFlowables(accounts))
                .flatMap { asset ->
                    when (asset) {
                        is AssetBalance -> handleNewAddedTokens(asset, accounts)
                            .also { assetBalances.add(asset) }
                            .filter { assetBalance ->
                                !assetBalance.accountToken.token.type.isSuperToken()
                            }
                        else -> Flowable.just(asset as AssetError)
                    }
                }
        }

    private fun handleNewAddedTokens(assetBalance: AssetBalance, accounts: List<Account>): Flowable<AssetBalance> =
        if (isAccountAddressMissing(assetBalance)) {
            accounts.forEach { account ->
                if (isTokenWithPositiveBalance(account, assetBalance)) {
                    newTokens.add(
                        assetBalance.accountToken.token.copy(
                            accountAddress = account.address,
                            tag = assetBalance.accountToken.token.tag,
                            type = assetBalance.accountToken.token.type
                        )
                    )
                }
            }
            Flowable.just(assetBalance)
        } else {
            Flowable.just(assetBalance)
        }

    private fun getTokenBalanceFlowables(accounts: List<Account>): List<Flowable<Asset>> =
        mutableListOf<Flowable<Asset>>().apply {
            accounts.forEach { account -> add(tokenManager.getTokenBalance(account).subscribeOn(Schedulers.io())) }
        }

    private fun isAccountAddressMissing(assetBalance: AssetBalance): Boolean =
        assetBalance.accountToken.currentBalance > BigDecimal.ZERO && assetBalance.accountToken.token.accountAddress.isBlank()

    override fun updateTokens(): Completable {
        return if (newTokens.isNotEmpty()) {
            val walletConfig = walletConfigManager.getWalletConfig()
            return walletConfigManager.updateWalletConfig(
                walletConfig.copy(
                    version = walletConfig.updateVersion,
                    erc20Tokens = getTokensWithAccountAddress(walletConfig.erc20Tokens)
                )

            ).doOnComplete { newTokens.clear() }
        } else {
            Completable.complete()
        }
    }

    internal fun getTokensWithAccountAddress(erc20Tokens: Map<Int, List<ERCToken>>): Map<Int, List<ERCToken>> {
        val allLocalTokensMap: MutableMap<Int, List<ERCToken>> = erc20Tokens.toMutableMap()
        val newTokensMap: Map<Int, List<ERCToken>> = newTokens.groupBy { token -> token.chainId }

        for ((chainId, newTokens) in newTokensMap) {
            val localTokensPerChainId = allLocalTokensMap[chainId] ?: listOf()
            val mergedTokens = mergeNewTokensWithLocal(localTokensPerChainId, newTokens)
            allLocalTokensMap[chainId] = mergedTokens
        }
        return allLocalTokensMap
    }

    private fun mergeNewTokensWithLocal(localChainTokens: List<ERCToken>, newTokens: List<ERCToken>) =
        mutableListOf<ERCToken>().apply {
            addAll(localChainTokens)
            newTokens.forEach { newToken -> add(newToken) }
        }

    private fun isTokenWithPositiveBalance(account: Account, assetBalance: AssetBalance): Boolean =
        account.privateKey.equals(assetBalance.privateKey, true) &&
                account.chainId == assetBalance.chainId && assetBalance.accountToken.currentBalance > BigDecimal.ZERO

    private val accountsForTokenBalanceRefresh: List<Account>
        get() = if (shouldGetAllAccounts()) {
            getActiveAccountsOnTestAndMainNets()
        } else {
            getActiveAccounts()
        }

    private fun getActiveAccountsOnTestAndMainNets() =
        walletConfigManager.getWalletConfig().accounts
            .filter { account -> refreshBalanceFilter(account) && account.network.isAvailable() }

    private fun shouldGetAllAccounts(): Boolean =
        walletConfigManager.getWalletConfig().erc20Tokens.values
            .any { tokens -> tokens.find { token -> token.accountAddress.isBlank() } != null }

    override fun getActiveAccounts(): List<Account> =
        walletConfigManager.getWalletConfig()
            .accounts.filter { account -> accountsFilter(account) && account.network.isAvailable() }

    override fun getActiveAccountsWithSuperfluidSupport(): List<Account> =
        getActiveAccounts()
            .filter { account -> account.hasSuperfluidSupport() }

    private fun accountsFilter(account: Account) =
        refreshBalanceFilter(account) && account.network.testNet == !localStorage.areMainNetworksEnabled

    private fun refreshBalanceFilter(account: Account) = !account.isHide && !account.isDeleted && !account.isPending
    override fun getTokensUpdate(): Flowable<List<ERCToken>> = tokenManager.getTokensUpdate()

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
                                .flatMap { (_, newAndLocalTokensPerChainIdMap) ->
                                    tokenManager.saveTokens(true, newAndLocalTokensPerChainIdMap)
                                        .onErrorReturn {
                                            Timber.e(it)
                                            false
                                        }
                                }
                        }
                }
        }

    private fun downloadTokensListWithBuffer(accounts: List<Account>): Single<List<ERCToken>> =
        Observable.fromIterable(accounts)
            .buffer(ETHERSCAN_REQUEST_TIMESPAN, TimeUnit.SECONDS, ETHERSCAN_REQUEST_PACKAGE)
            .flatMapSingle { accountList -> downloadTokensList(accountList) }
            .toList()
            .map { tokens -> mergeLists(tokens) }

    private fun downloadTokensList(accounts: List<Account>): Single<List<ERCToken>> =
        Observable.fromIterable(accounts)
            .flatMapSingle { account ->
                tokenManager.downloadTokensList(account)
                    .onErrorReturn {
                        Timber.e(it, "Error while downloading token list")
                        emptyList()
                    }
            }
            .toList()
            .map { tokens -> mergeLists(tokens) }

    private fun mergeLists(lists: List<List<ERCToken>>): List<ERCToken> =
        mutableListOf<ERCToken>().apply {
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
        }.flatMap { ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty } }
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun getTransactions(): Single<List<PendingAccount>> =
        blockchainRepository.getTransactions(getTxHashes())
            .map { pendingTxs -> getPendingAccountsWithBlockHashes(pendingTxs) }

    override fun getCoinFiatRate(chainId: Int): Single<Double> =
        currentFiatCurrency.let { currentFiat ->
            when (chainId) {
                ETH_MAIN -> fetchCoinRate(MarketIds.ETHEREUM).map { markets -> markets.ethFiatPrice?.getRate(currentFiat) }
                ARB_ONE -> fetchCoinRate(MarketIds.ETHEREUM).map { markets -> markets.ethFiatPrice?.getRate(currentFiat) }
                OPT -> fetchCoinRate(MarketIds.ETHEREUM).map { markets -> markets.ethFiatPrice?.getRate(currentFiat) }
                POA_CORE -> fetchCoinRate(MarketIds.POA_NETWORK).map { markets -> markets.poaFiatPrice?.getRate(currentFiat) }
                GNO -> fetchCoinRate(MarketIds.GNO).map { markets -> markets.daiFiatPrice?.getRate(currentFiat) }
                POLYGON -> fetchCoinRate(MarketIds.POLYGON).map { markets -> markets.polygonFiatPrice?.getRate(currentFiat) }
                BSC -> fetchCoinRate(MarketIds.BSC_COIN).map { markets -> markets.bscFiatPrice?.getRate(currentFiat) }
                CELO -> fetchCoinRate(MarketIds.CELO).map { markets -> markets.celoFiatPrice?.getRate(currentFiat) }
                AVA_C -> fetchCoinRate(MarketIds.AVAX).map { markets -> markets.avaxFiatPrice?.getRate(currentFiat) }
                ZKS_ERA -> fetchCoinRate(MarketIds.ETHEREUM).map { markets -> markets.ethFiatPrice?.getRate(currentFiat) }
                ZK_EVM -> fetchCoinRate(MarketIds.ETHEREUM).map { markets -> markets.ethFiatPrice?.getRate(currentFiat) }
                else -> Single.just(ZERO_FIAT_VALUE)
            }
        }

    override fun getTokenFiatRate(tokenHash: String): Single<Double> =
        Single.just(tokenManager.getSingleTokenRate(tokenHash))

    override fun toUserReadableFormat(value: BigDecimal): BigDecimal = unitConverter.toEther(value)

    override fun sendTransaction(chainId: Int, transaction: Transaction): Single<String> =
        blockchainRepository.sendWalletConnectTransaction(
            chainId,
            TransactionToTransactionPayloadMapper.map(transaction)
        )

    override fun getFiatSymbol(): String = Fiat.getFiatSymbol(currentFiatCurrency)

    override fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost> = with(txCostPayload) {
        when {
            shouldGetGasPriceFromApi(chainId) -> {
                cryptoApi.getGasPriceFromRpcOverHttp(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                    .flatMap { gasPrice ->
                        var gasPriceResult = gasPrice.result
                        if (gasPriceResult?.toBigInteger()!! < NetworkManager.getNetwork(chainId).minGasPrice) {
                            gasPriceResult = NetworkManager.getNetwork(chainId).minGasPrice.toBigDecimal()
                        }
                        getTxCosts(txCostPayload, null, gasPriceResult)
                    }
                    .onErrorResumeNext {
                        getTxCosts(txCostPayload, null, null)
                    }
            }
            else -> getTxCosts(txCostPayload, null, null)
        }
    }

    override fun isAddressValid(address: String, chainId: Int?): Boolean = validationRepository.isAddressValid(address, chainId)

    override fun toRecipientChecksum(address: String, chainId: Int?): String =
        validationRepository.toRecipientChecksum(address, chainId)

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.run { getTransactionCostInEth(unitConverter.toGwei(gasPrice), BigDecimal(gasLimit)) }

    override fun shouldOpenNewWssConnection(accountIndex: Int): Boolean {
        val chainId = getPendingAccount(accountIndex).chainId
        return when {
            getPendingAccounts().size == ONE_PENDING_ACCOUNT -> isFirstAccountPending(accountIndex, chainId)
            getPendingAccounts().size > ONE_PENDING_ACCOUNT -> isNetworkAlreadyPending(chainId)
            else -> false
        }
    }

    @Throws(ConnectException::class)
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
        localStorage.getPendingAccounts().find { it.index == accountIndex }
            .orElse { PendingAccount(Int.InvalidIndex) }

    override fun clearPendingAccounts() {
        localStorage.clearPendingAccounts()
    }

    override fun getPendingAccounts(): List<PendingAccount> = localStorage.getPendingAccounts()

    override fun transferERC20Token(chainId: Int, transaction: Transaction): Completable =
        erC20TokenRepository.transferERC20Token(chainId, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun transferERC721Token(chainId: Int, transaction: Transaction): Completable =
        erc721TokenRepository.transferERC721Token(chainId, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun transferERC1155Token(chainId: Int, transaction: Transaction): Completable =
        erc1155TokenRepository.transferERC1155Token(chainId, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(ensRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun loadRecipients(): List<Recipient> = localStorage.getRecipients()
    override fun resolveENS(ensName: String): Single<String> = ensRepository.resolveENS(ensName)
    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)
    override fun getAccountByAddressAndChainId(address: String, chainId: Int): Account? =
        walletConfigManager.getWalletConfig().accounts.find { account ->
            account.address.equals(address, true) && account.chainId == chainId
        }

    override fun getFreeATS(address: String) = blockchainRepository.getFreeATS(address)
    override fun checkMissingTokensDetails(): Completable = tokenManager.checkMissingTokensDetails()
    override fun isProtectTransactionEnabled(): Boolean = localStorage.isProtectTransactionsEnabled
    private fun shouldGetGasPriceFromApi(chainId: Int) =
        NetworkManager.getNetwork(chainId).gasPriceOracle.isNotEmpty()

    private fun getTxCosts(
        payload: TxCostPayload,
        speed: TransactionSpeed?,
        defaultGasPrice: BigDecimal?
    ): Single<TransactionCost> =
        blockchainRepository.getTransactionCosts(TxCostPayloadToTxCostDataMapper.map(payload), defaultGasPrice)
            .map { txCost ->
                TransactionCostPayloadToTransactionCost.map(txCost, speed, payload.chainId) {
                    unitConverter.fromWei(it).setScale(SCALE, RoundingMode.HALF_EVEN)
                }
            }

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

    private fun saveRecipient(ensName: String, address: String) =
        localStorage.saveRecipient(Recipient(ensName, address))

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val PENDING_NETWORK_LIMIT = 2
        private const val ETHERSCAN_REQUEST_TIMESPAN = 1L
        private const val ETHERSCAN_REQUEST_PACKAGE = 5
        private const val ZERO_FIAT_VALUE = 0.0
        private const val SCALE = 0
    }
}