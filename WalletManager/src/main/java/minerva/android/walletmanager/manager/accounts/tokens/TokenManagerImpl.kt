package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.TokenDetails
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.walletmanager.BuildConfig.*
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.database.dao.TokenDao
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.mappers.TokenDataToERC20Token
import minerva.android.walletmanager.model.mappers.TokenDetailsToERC20TokensMapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenTag
import minerva.android.walletmanager.model.token.Tokens
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.utils.MarketUtils
import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import java.util.*

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val rateStorage: RateStorage,
    database: MinervaDatabase
) : TokenManager {
    private val currentTimeProvider = CurrentTimeProviderImpl()
    private val tokenDao: TokenDao = database.tokenDao()
    private var currentFiat = String.Empty

    override fun saveToken(chainId: Int, token: ERC20Token): Completable =
        walletManager.getWalletConfig().run {
            copy(version = updateVersion, erc20Tokens = updateTokens(chainId, token, erc20Tokens.toMutableMap()))
                .let { walletConfig -> walletManager.updateWalletConfig(walletConfig) }
        }

    override fun saveTokens(
        shouldBeSaved: Boolean,
        newAndLocalTokensPerChainIdMap: Map<Int, List<ERC20Token>>
    ): Single<Boolean> =
        if (shouldBeSaved) {
            walletManager.getWalletConfig()
                .run {
                    copy(
                        version = updateVersion,
                        erc20Tokens = updateTokens(newAndLocalTokensPerChainIdMap)
                    ).let { walletConfig -> walletManager.updateWalletConfig(walletConfig) }
                        .toSingle { shouldBeSaved }
                        .onErrorReturn { shouldBeSaved }
                }
        } else Single.just(shouldBeSaved)

    override fun checkMissingTokensDetails(): Completable =
        cryptoApi.getLastCommitFromTokenList(url = ERC20_TOKEN_DATA_LAST_COMMIT)
            .zipWith(tokenDao.getTaggedTokens())
            .filter { (commits, tokens) -> isNewCommit(commits) || tokens.isEmpty() }
            .flatMapSingle { getMissingTokensDetails() }
            .map { tokenDetailsMap ->
                tokenDao.updateTaggedTokens(TokenDetailsToERC20TokensMapper.map(filterTaggedTokens(tokenDetailsMap)))
                tokenDetailsMap
            }
            .flatMapCompletable { tokenDetailsMap -> updateTokensIcons(tokenDetailsMap) }
            .doOnComplete { localStorage.saveTokenIconsUpdateTimestamp(currentTimeProvider.currentTimeMills()) }

    private fun filterTaggedTokens(tokenDetailsMap: Map<String, TokenDetails>): List<TokenDetails> =
        tokenDetailsMap.values.toList().filter { tokenDetails ->
            tokenDetails.tags.isNotEmpty() && (tokenDetails.tags.contains(TokenTag.SUPER_TOKEN.tag) ||
                    tokenDetails.tags.contains(TokenTag.WRAPPER_TOKEN.tag))
        }

    private fun isNewCommit(list: List<CommitElement>): Boolean =
        list[LAST_UPDATE_INDEX].lastCommitDate.let {
            localStorage.loadTokenIconsUpdateTimestamp() < DateUtils.getTimestampFromDate(it)
        }

    private fun getMissingTokensDetails(): Single<Map<String, TokenDetails>> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL)
            .map { tokens ->
                tokens.associateBy { tokenDetails ->
                    generateTokenHash(tokenDetails.chainId, tokenDetails.address)
                }
            }

    private fun updateTokensIcons(tokens: Map<String, TokenDetails>): Completable =
        walletManager.getWalletConfig().run {
            erc20Tokens.forEach { (id, tokenList) ->
                tokenList.forEach { token ->
                    token.logoURI = tokens[generateTokenHash(id, token.address)]?.logoURI
                }
            }
            walletManager.updateWalletConfig(copy(version = updateVersion))
        }

    override fun getTokenIconURL(chainId: Int, address: String): Single<String> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.find { tokenDetails ->
                chainId == tokenDetails.chainId && address == tokenDetails.address
            }?.logoURI ?: String.Empty
        }

    override fun loadCurrentTokensPerNetwork(account: Account): List<ERC20Token> =
        walletManager.getWalletConfig().run {
            NetworkManager.getTokens(account.chainId)
                .mergeWithoutDuplicates(erc20Tokens[account.chainId] ?: listOf())
        }

    private fun getTokensPerAccount(account: Account): List<ERC20Token> {
        val localTokensPerNetwork = NetworkManager.getTokens(account.chainId)
        val remoteTokensPerNetwork = walletManager.getWalletConfig().erc20Tokens[account.chainId] ?: listOf()
        if (remoteTokensPerNetwork.isNotEmpty() && remoteTokensPerNetwork.all { token -> token.accountAddress.isEmpty() }) {
            return loadCurrentTokensPerNetwork(account)
        } else {
            return mutableListOf<ERC20Token>().apply {
                addAll(remoteTokensPerNetwork.filter { remoteToken ->
                    remoteToken.accountAddress.equals(account.address, true)
                })
                if (isEmpty() && localTokensPerNetwork.isNotEmpty()) {
                    addAll(localTokensPerNetwork)
                    return@apply
                } else if (isEmpty()) {
                    return@apply
                }
                localTokensPerNetwork.forEach { localToken ->
                    if (find { remoteToken -> remoteToken.address.equals(localToken.address, true) } == null) {
                        add(localToken)
                    }
                }
            }
        }
    }

    override fun refreshTokensBalances(account: Account): Single<Pair<String, List<AccountToken>>> =
        tokenDao.getTaggedTokens()
            .zipWith(Single.just(getTokensPerAccount(account)))
            .flatMap { (taggedTokens, tokensPerAccount) ->
                fillActiveTokensWithTags(taggedTokens, account, tokensPerAccount)
                val tokens = tokensPerAccount.mergeWithoutDuplicates(taggedTokens)
                    .filter { token -> token.chainId == account.chainId }
                Observable.fromIterable(tokens)
                    .flatMap { token ->
                        blockchainRepository.refreshTokenBalance(
                            account.privateKey,
                            account.chainId,
                            token.address,
                            account.address
                        )
                    }
                    .map { (tokenAddress, balance) ->
                        tokens.find { token -> token.address.equals(tokenAddress, true) }?.let { erc20Token ->
                            AccountToken(
                                erc20Token,
                                balance,
                                rateStorage.getRate(generateTokenHash(erc20Token.chainId, erc20Token.address))
                            )
                        }.orElse { throw NullPointerException() }
                    }
                    .toList()
                    .map { accountTokens -> Pair(account.privateKey, accountTokens) }
            }

    private fun fillActiveTokensWithTags(
        taggedTokens: List<ERC20Token>,
        account: Account,
        activeTokens: List<ERC20Token>
    ) {
        taggedTokens
            .filter { taggedToken -> taggedToken.chainId == account.chainId }
            .forEach { taggedToken ->
                activeTokens.filter { activeToken -> activeToken.address.equals(taggedToken.address, true) }
                    .forEach { activeToken ->
                        with(activeToken) {
                            if (tag.isEmpty()) {
                                tag = taggedToken.tag
                                accountAddress = String.Empty
                            }
                        }
                    }

            }
    }

    override fun sortTokensByChainId(tokenList: List<ERC20Token>): Map<Int, List<ERC20Token>> =
        mutableMapOf<Int, MutableList<ERC20Token>>().apply {
            tokenList.forEach { token ->
                get(token.chainId)?.add(token)
                    .orElse { put(token.chainId, mutableListOf(token)) }
            }
        }

    override fun getTaggedTokensUpdate(): Flowable<List<ERC20Token>> = tokenDao.getTaggedTokensFlowable()
    override fun getSingleTokenRate(tokenHash: String): Double = rateStorage.getRate(tokenHash)

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.associate { tokenDetails ->
                generateTokenHash(tokenDetails.chainId, tokenDetails.address) to tokenDetails.logoURI
            }
        }

    override fun downloadTokensList(account: Account): Single<List<ERC20Token>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> getEthereumTokens(account)
            MATIC, MUMBAI -> Single.just(emptyList()) // Networks without token explorer urls
            else -> getNotEthereumTokens(account)
        }

    private fun prepareContractAddresses(tokens: List<ERC20Token>): String =
        tokens.joinToString(TOKEN_ADDRESS_SEPARATOR) { token -> token.address }

    override fun getTokensRates(tokens: Map<Int, List<ERC20Token>>): Completable =
        mutableListOf<Observable<List<Pair<String, Double>>>>().let { observables ->
            with(localStorage.loadCurrentFiat()) {
                if (currentFiat != this) rateStorage.clearRates()
                tokens.forEach { (chainId, tokens) ->
                    val marketId = MarketUtils.getMarketId(chainId)
                    if (!rateStorage.areRatesSynced && marketId != String.Empty) {
                        observables.add(updateAccountTokensRate(marketId, chainId, prepareContractAddresses(tokens)))
                    }
                }
                Observable.merge(observables)
                    .doOnNext { rates ->
                        rates.forEach { (fiatSymbol, rate) ->
                            rateStorage.saveRate(fiatSymbol, rate)
                        }
                    }.toList()
                    .doOnSuccess {
                        currentFiat = this
                        rateStorage.areRatesSynced = true
                    }
                    .ignoreElement()
            }
        }

    private fun updateAccountTokensRate(
        marketId: String,
        chainId: Int,
        contractAddresses: String
    ): Observable<List<Pair<String, Double>>> =
        with(localStorage.loadCurrentFiat()) {
            cryptoApi.getTokensRate(marketId, contractAddresses, this)
                .map { tokenRateResponse ->
                    mutableListOf<Pair<String, Double>>().apply {
                        tokenRateResponse.forEach { (contractAddress, rate) ->
                            add(
                                Pair(
                                    generateTokenHash(chainId, contractAddress),
                                    rate[toLowerCase(Locale.ROOT)]?.toDouble() ?: Double.InvalidValue
                                )
                            )
                        }
                    }.toList()
                }.toObservable()
        }

    override fun updateTokensRate(account: Account) {
        account.apply {
            accountTokens.forEach { accountToken ->
                with(accountToken) {
                    tokenPrice = rateStorage.getRate(generateTokenHash(token.chainId, token.address))
                }
            }
        }
    }

    private fun getNotEthereumTokens(account: Account): Single<List<ERC20Token>> =
        cryptoApi.getConnectedTokens(url = getTokensApiURL(account))
            .map { response ->
                mutableListOf<ERC20Token>().apply {
                    response.tokens
                        .filter { tokenData -> tokenData.type == Tokens.ERC_20.type }
                        .forEach { tokenData ->
                            add(TokenDataToERC20Token.map(account.chainId, tokenData, account.address))
                        }
                }
            }

    private fun getEthereumTokens(account: Account): Single<List<ERC20Token>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map { response ->
                mutableListOf<ERC20Token>().apply {
                    response.tokens.forEach { tokenTx ->
                        add(ERC20Token(account.chainId, tokenTx, account.address))
                    }
                }
            }

    private fun getTokenTxApiURL(account: Account): String =
        String.format(ETHEREUM_TOKENTX_REQUEST, getTokenBalanceURL(account.chainId), account.address, ETHERSCAN_KEY)

    @VisibleForTesting
    fun getTokensApiURL(account: Account): String =
        String.format(TOKEN_BALANCE_REQUEST, getTokenBalanceURL(account.chainId), account.address)

    private fun getTokenBalanceURL(chainId: Int) =
        when (chainId) {
            ETH_MAIN -> ETHEREUM_MAINNET_TOKEN_BALANCE_URL
            ETH_RIN -> ETHEREUM_RINKEBY_TOKEN_BALANCE_URL
            ETH_ROP -> ETHEREUM_ROPSTEN_TOKEN_BALANCE_URL
            ETH_KOV -> ETHEREUM_KOVAN_TOKEN_BALANCE_URL
            ETH_GOR -> ETHEREUM_GOERLI_TOKEN_BALANCE_URL
            ATS_TAU -> ARTIS_TAU_TOKEN_BALANCE_URL
            ATS_SIGMA -> ARTIS_SIGMA_TOKEN_BALANCE_URL
            POA_SKL -> POA_SOKOL_TOKEN_BALANCE_URL
            POA_CORE -> POA_CORE_TOKEN_BALANCE_URL
            XDAI -> X_DAI_TOKEN_BALANCE_URL
            LUKSO_14 -> LUKSO_TOKEN_BALANCE_URL
            else -> throw NetworkNotFoundThrowable()
        }

    /**
     * arguments: tokens MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    @VisibleForTesting
    fun updateTokens(
        chainId: Int,
        newToken: ERC20Token,
        tokens: MutableMap<Int, List<ERC20Token>>
    ): Map<Int, List<ERC20Token>> =
        tokens.apply {
            (this[chainId] ?: listOf()).toMutableList().let { currentTokens ->
                currentTokens.add(newToken)
                put(chainId, currentTokens)
                rateStorage.areRatesSynced = false
            }
        }


    override fun mergeWithLocalTokensList(newTokensPerChainIdMap: Map<Int, List<ERC20Token>>): Pair<Boolean, Map<Int, List<ERC20Token>>> =
        walletManager.getWalletConfig().erc20Tokens.let { allLocalTokens ->
            var updateLogosURI = false
            val allLocalTokensMap = allLocalTokens.toMutableMap()
            for ((chainId, newTokens) in newTokensPerChainIdMap) {
                val localChainTokens = allLocalTokensMap[chainId] ?: listOf()
                mergeNewTokensWithLocal(localChainTokens, newTokens)
                    .let { tokenList ->
                        updateLogosURI = localChainTokens.size != tokenList.size
                        allLocalTokensMap[chainId] = tokenList
                    }
            }
            Pair(updateLogosURI, allLocalTokensMap)
        }

    private fun mergeNewTokensWithLocal(localChainTokens: List<ERC20Token>, newTokens: List<ERC20Token>) =
        mutableListOf<ERC20Token>().apply {
            addAll(localChainTokens)
            newTokens.forEach { newToken ->
                if (isNewToken(newToken)) {
                    add(newToken)
                } else if (isNewTokenForAccount(newToken)) {
                    add(newToken)
                }
            }
        }

    private fun MutableList<ERC20Token>.isNewTokenForAccount(newToken: ERC20Token) =
        find { localToken ->
            localToken.address.equals(newToken.address, true) &&
                    localToken.accountAddress.equals(newToken.accountAddress, true)
        } == null

    private fun MutableList<ERC20Token>.isNewToken(newToken: ERC20Token) =
        find { localToken -> localToken.address.equals(newToken.address, true) } == null


    override fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERC20Token>>
    ): Single<Pair<Boolean, Map<Int, List<ERC20Token>>>> =
        if (shouldBeUpdated) {
            getTokenIconsURL().map { logoUrls ->
                tokensPerChainIdMap.values.forEach { tokens ->
                    tokens.forEach { token ->
                        token.apply {
                            logoUrls[generateTokenHash(chainId, address)]?.let { newLogoURI ->
                                logoURI = newLogoURI
                            }
                        }
                    }
                }
                Pair(true, tokensPerChainIdMap)
            }
        } else Single.just(Pair(false, tokensPerChainIdMap))

    /**
     *
     * arguments: map - downloaded tokens - Map<AccountPrivateKey, List<AccountToken>>, tokens - MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    @VisibleForTesting
    fun updateTokens(newTokensWithIcons: Map<Int, List<ERC20Token>>): Map<Int, List<ERC20Token>> {
        return mutableMapOf<Int, List<ERC20Token>>().apply {
            for ((chainId, tokens) in newTokensWithIcons) {
                tokens.onEach { token -> token.accountAddress = token.accountAddress.toLowerCase(Locale.ROOT) }
                this[chainId] = tokens.distinct()
            }
            rateStorage.areRatesSynced = false
        }
    }

    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val TOKEN_BALANCE_REQUEST = "%sapi?module=account&action=tokenlist&address=%s"
        private const val ETHEREUM_TOKENTX_REQUEST =
            "%sapi?module=account&action=tokentx&address=%s&startblock=0&endblock=999999999&sort=asc&apikey=%s"
        private const val TOKEN_ADDRESS_SEPARATOR = ","
    }
}