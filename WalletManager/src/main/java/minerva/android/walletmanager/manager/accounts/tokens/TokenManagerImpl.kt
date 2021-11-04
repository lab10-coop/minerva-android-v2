package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.TokenDetails
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.superToken.SuperTokenRepository
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
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC_TESTNET
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
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_TEST
import minerva.android.walletmanager.model.defs.ChainId.Companion.XDAI
import minerva.android.walletmanager.model.mappers.TokenDataToERCToken
import minerva.android.walletmanager.model.mappers.TokenDetailsToERC20TokensMapper
import minerva.android.walletmanager.model.mappers.TokenToAssetBalanceErrorMapper
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.*
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.utils.MarketUtils
import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val superTokenRepository: SuperTokenRepository,
    private val erc20TokenRepository: ERC20TokenRepository,
    private val erc721TokenRepository: ERC721TokenRepository,
    private val rateStorage: RateStorage,
    database: MinervaDatabase
) : TokenManager {
    override var activeSuperTokenStreams: MutableList<ActiveSuperToken> = mutableListOf()
    private val currentTimeProvider = CurrentTimeProviderImpl()
    private val tokenDao: TokenDao = database.tokenDao()
    private var currentFiat = String.Empty

    override fun saveToken(accountAddress: String, chainId: Int, token: ERCToken): Completable =
        tokenDao.getTaggedTokens()
            .flatMapCompletable { tokens ->
                var tag = String.Empty
                tokens.find { taggedToken -> taggedToken.address.equals(token.address, true) }
                    ?.let { tag = it.tag }
                walletManager.getWalletConfig().run {
                    copy(
                        version = updateVersion,
                        erc20Tokens = updateTokens(
                            chainId, token.copy(accountAddress = accountAddress, tag = tag),
                            erc20Tokens.toMutableMap()
                        )
                    ).let { walletConfig -> walletManager.updateWalletConfig(walletConfig) }
                }
            }


    override fun saveTokens(
        shouldSafeNewTokens: Boolean,
        newAndLocalTokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<Boolean> =
        if (shouldSafeNewTokens) {
            walletManager.getWalletConfig()
                .run {
                    copy(
                        version = updateVersion,
                        erc20Tokens = updateTokens(newAndLocalTokensPerChainIdMap)
                    )
                        .let { walletConfig -> walletManager.updateWalletConfig(walletConfig) }
                        .toSingle { shouldSafeNewTokens }
                        .onErrorReturn { shouldSafeNewTokens }
                }
        } else Single.just(shouldSafeNewTokens)

    override fun checkMissingTokensDetails(): Completable =
        cryptoApi.getLastCommitFromTokenList(url = ERC20_TOKEN_DATA_LAST_COMMIT)
            .zipWith(tokenDao.getTaggedTokens())
            .filter { (commits, tokens) -> isNewCommit(commits) || tokens.isEmpty() }
            .flatMapSingle { getMissingTokensDetails() }
            .map { tokenDetailsMap ->
                tokenDao.updateTaggedTokens(
                    TokenDetailsToERC20TokensMapper.map(
                        filterTaggedTokens(
                            tokenDetailsMap
                        )
                    )
                )
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

    override fun getActiveTokensPerAccount(account: Account): List<ERCToken> =
        walletManager.getWalletConfig().erc20Tokens[account.chainId]
            ?.filter { token -> token.accountAddress.equals(account.address, true) } ?: listOf()

    override fun getNftsPerAccount(
        chainId: Int,
        accountAddress: String,
        collectionAddress: String
    ): List<ERCToken> = walletManager.getWalletConfig().erc20Tokens[chainId]
        ?.filter { token ->
            token.accountAddress.equals(accountAddress, true) && token.address.equals(
                collectionAddress,
                true
            ) && token.type.isERC721()
        } ?: listOf()

    private fun getAllTokensPerAccount(account: Account): List<ERCToken> {
        val localTokensPerNetwork = NetworkManager.getTokens(account.chainId)
        val remoteTokensPerNetwork =
            walletManager.getWalletConfig().erc20Tokens[account.chainId] ?: listOf()
        if (remoteTokensPerNetwork.isNotEmpty() && remoteTokensPerNetwork.all { token -> token.accountAddress.isEmpty() }) {
            return remoteTokensPerNetwork
        } else {
            return mutableListOf<ERCToken>().apply {
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
                    if (find { remoteToken ->
                            remoteToken.address.equals(
                                localToken.address,
                                true
                            )
                        } == null) {
                        add(localToken)
                    }
                }
            }
        }
    }

    override fun getSuperTokenBalance(account: Account): Flowable<Asset> {
        with(account) {
            return tokenDao.getTaggedTokens()
                .zipWith(Single.just(getAllTokensPerAccount(this)))
                .flatMapPublisher { (taggedTokens, tokensPerAccount) ->
                    fillActiveTokensWithTags(taggedTokens, this, tokensPerAccount)
                    Flowable.mergeDelayError(
                        getSuperTokenBalanceFlowables(
                            activeSuperTokenStreams,
                            this
                        )
                    )
                        .flatMap { superTokenBalance ->
                            getSuperTokenNetFlow(superTokenBalance, account)
                                .map { netFlow ->
                                    handleTokensBalances(
                                        superTokenBalance,
                                        getSuperTokensForAccount(tokensPerAccount, taggedTokens),
                                        account,
                                        netFlow
                                    )
                                }
                        }
                }
        }
    }

    private fun handleTokensBalances(
        token: Token,
        tokens: List<ERCToken>,
        account: Account,
        netFlow: BigInteger = BigInteger.ZERO
    ): Asset =
        when (token) {
            is TokenWithBalance -> getAssetBalance(tokens, token, account, netFlow)
            else -> TokenToAssetBalanceErrorMapper.map(account, token as TokenWithError)
        }

    private fun getAssetBalance(
        tokens: List<ERCToken>,
        tokenWithBalance: TokenWithBalance,
        account: Account,
        netFlow: BigInteger
    ): AssetBalance =
        tokens.find { token -> token.address.equals(tokenWithBalance.address, true) }
            ?.let { token ->
                val isStreamActive = isActiveSuperToken(tokenWithBalance, account)
                AssetBalance(
                    account.chainId,
                    account.privateKey,
                    getAccountToken(
                        token.copy(isStreamActive = isStreamActive, consNetFlow = netFlow),
                        tokenWithBalance.balance,
                        isStreamActive
                    )
                )
            }
            .orElse { throw NullPointerException() }

    private fun getAccountToken(
        erc20Token: ERCToken,
        balance: BigDecimal,
        isStreamActive: Boolean
    ): AccountToken =
        if (isStreamActive) {
            AccountToken(
                erc20Token,
                tokenPrice = rateStorage.getRate(
                    generateTokenHash(
                        erc20Token.chainId,
                        erc20Token.address
                    )
                ),
                nextRawBalance = balance,
                currentRawBalance = balance
            )
        } else {
            AccountToken(
                erc20Token,
                currentRawBalance = balance,
                tokenPrice = rateStorage.getRate(
                    generateTokenHash(
                        erc20Token.chainId,
                        erc20Token.address
                    )
                )
            )
        }

    private fun isActiveSuperToken(tokenWithBalance: TokenWithBalance, account: Account): Boolean =
        activeSuperTokenStreams.any { superToken ->
            superToken.address.equals(tokenWithBalance.address, true) &&
                    superToken.accountAddress.equals(account.address, true)
        }

    private fun Account.getSuperTokensForAccount(
        tokensPerAccount: List<ERCToken>,
        taggedTokens: List<ERCToken>
    ): List<ERCToken> =
        getTokensForAccount(tokensPerAccount, taggedTokens, this)
            .filter { token -> token.tag == TokenTag.SUPER_TOKEN.tag }

    private fun getSuperTokenBalanceFlowables(
        tokens: List<ActiveSuperToken>,
        account: Account
    ): List<Flowable<Token>> =
        with(account) {
            return mutableListOf<Flowable<Token>>().apply {
                tokens.forEach { token ->
                    add(
                        erc20TokenRepository.getTokenBalance(
                            privateKey,
                            chainId,
                            token.address,
                            address
                        )
                            .subscribeOn(Schedulers.io())
                    )
                }
            }
        }

    override fun getTokenBalance(account: Account): Flowable<Asset> {
        with(account) {
            return tokenDao.getTaggedTokens()
                .zipWith(Single.just(getAllTokensPerAccount(this)))
                .flatMapPublisher { (taggedTokens, tokensPerAccount) ->
                    fillActiveTokensWithTags(taggedTokens, this, tokensPerAccount)
                    val tokens = getTokensForAccount(tokensPerAccount, taggedTokens, this)
                    Flowable.mergeDelayError(getTokenBalanceFlowables(tokens, this))
                        .flatMap { (token, tag) ->
                            if (token is TokenWithBalance) {
                                handleActiveSuperTokens(tag, account, token)
                            } else {
                                Flowable.just(token)
                            }
                        }
                        .map { token -> handleTokensBalances(token, tokens, account) }
                }
        }
    }

    private fun handleActiveSuperTokens(tag: String, account: Account, token: TokenWithBalance) =
        if (isSuperFluidToken(tag, account)) {
            getSuperTokenNetFlow(token, account)
                .map { netFlow ->
                    if (netFlow != BigInteger.ZERO) {
                        addActiveSuperToken(token, account)
                    } else {
                        removeActiveSuperToken(token, account)
                    }
                    token
                }
        } else {
            Flowable.just(token)
        }

    private fun getSuperTokenNetFlow(token: Token, account: Account) = with(account) {
        superTokenRepository.getNetFlow(
            network.superfluid!!.cfav1,
            chainId,
            privateKey,
            token.address,
            address
        )
    }

    private fun removeActiveSuperToken(
        token: Token,
        account: Account
    ) {
        activeSuperTokenStreams.remove(
            ActiveSuperToken(
                token.address,
                account.address,
                account.chainId
            )
        )
    }

    private fun addActiveSuperToken(
        token: Token,
        account: Account
    ) {
        activeSuperTokenStreams.add(
            ActiveSuperToken(
                token.address,
                account.address,
                account.chainId
            )
        )
    }

    private fun isSuperFluidToken(tag: String, account: Account) =
        tag == TokenTag.SUPER_TOKEN.tag && account.network.superfluid != null && account.network.wsRpc != String.Empty

    private fun handleTokensBalances(token: Token, tokens: List<ERCToken>, account: Account): Asset =
        when (token) {
            is TokenWithBalance -> {
                getAssetBalance(tokens, token, account)
            }
            else -> TokenToAssetBalanceErrorMapper.map(account, token as TokenWithError)
        }

    private fun getAssetBalance(
        tokens: List<ERCToken>,
        tokenWithBalance: TokenWithBalance,
        account: Account
    ): AssetBalance =
        tokens.find { token -> token.address.equals(tokenWithBalance.address, true) && token.tokenId == tokenWithBalance.tokenId }
            ?.let { token ->
                AssetBalance(
                    account.chainId,
                    account.privateKey,
                    getAccountToken(
                        token.copy(isStreamActive = isActiveSuperToken(tokenWithBalance, account)),
                        tokenWithBalance.balance
                    )
                )
            }
            .orElse { throw NullPointerException() }

    private fun getTokenBalanceFlowables(
        tokens: List<ERCToken>,
        account: Account
    ): List<Flowable<Pair<Token, String>>> =
        with(account) {
            val tokenBalanceFlowables = mutableListOf<Flowable<Pair<Token, String>>>()
            tokens.forEach { ercToken ->
                if (ercToken.type.isERC721()) {
                    tokenBalanceFlowables.add(
                        erc721TokenRepository.getTokenBalance(ercToken.tokenId!!, privateKey, chainId, ercToken.address, address)
                            .map { token -> Pair(token, ercToken.tag) }
                            .subscribeOn(Schedulers.io())
                    )
                } else {
                    if (ercToken.decimals.isNotBlank()) {
                        tokenBalanceFlowables.add(
                            erc20TokenRepository.getTokenBalance(privateKey, chainId, ercToken.address, address)
                                .map { token -> Pair(token, ercToken.tag) }
                                .subscribeOn(Schedulers.io())
                        )
                    }
                }
            }
            return tokenBalanceFlowables
        }

    private fun getTokensForAccount(
        tokensPerAccount: List<ERCToken>,
        tagged: List<ERCToken>,
        account: Account
    ): List<ERCToken> =
        tokensPerAccount
            .mergeWithoutDuplicates(tagged)
            .filter { token -> token.chainId == account.chainId }

    private fun getAccountToken(ercToken: ERCToken, balance: BigDecimal): AccountToken =
        AccountToken(ercToken, balance, rateStorage.getRate(generateTokenHash(ercToken.chainId, ercToken.address)))

    private fun fillActiveTokensWithTags(taggedTokens: List<ERCToken>, account: Account, tokens: List<ERCToken>) {
        taggedTokens
            .filter { taggedToken -> taggedToken.chainId == account.chainId }
            .forEach { taggedToken ->
                tokens.filter { activeToken ->
                    activeToken.address.equals(
                        taggedToken.address,
                        true
                    )
                }
                    .forEach { activeToken ->
                        with(activeToken) {
                            if (tag.isEmpty()) {
                                tag = taggedToken.tag
                            }
                        }
                    }
            }
    }

    override fun getTaggedTokensUpdate(): Flowable<List<ERCToken>> = tokenDao.getTaggedTokensFlowable()
    override fun getTaggedTokensSingle(): Single<List<ERCToken>> = tokenDao.getTaggedTokens()
    override fun getSingleTokenRate(tokenHash: String): Double = rateStorage.getRate(tokenHash)

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.associate { tokenDetails ->
                generateTokenHash(
                    tokenDetails.chainId,
                    tokenDetails.address
                ) to tokenDetails.logoURI
            }
        }

    override fun downloadTokensList(account: Account): Single<List<ERCToken>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR, MATIC, BSC, BSC_TESTNET -> getTokensFromTx(account)
            MUMBAI, RSK_TEST, RSK_MAIN -> Single.just(emptyList()) // Networks without token explorer urls
            else -> getTokensForAccount(account)
        }

    private fun prepareContractAddresses(tokens: List<ERCToken>): String =
        tokens.joinToString(TOKEN_ADDRESS_SEPARATOR) { token -> token.address }

    override fun getTokensRates(tokens: Map<Int, List<ERCToken>>): Completable =
        mutableListOf<Observable<List<Pair<String, Double>>>>().let { observables ->
            with(localStorage.loadCurrentFiat()) {
                if (currentFiat != this && currentFiat != String.Empty) rateStorage.clearRates()
                tokens.forEach { (chainId, tokens) ->
                    val marketId = MarketUtils.getTokenGeckoMarketId(chainId)
                    if (!rateStorage.areRatesSynced && marketId != String.Empty) {
                        observables.add(
                            updateAccountTokensRate(
                                marketId,
                                chainId,
                                prepareContractAddresses(tokens.distinctBy { it.address })
                            )
                        )
                    }
                }
                Observable.merge(observables)
                    .doOnNext { rates ->
                        rates.forEach { (fiatSymbol, rate) ->
                            rateStorage.saveRate(
                                fiatSymbol,
                                rate
                            )
                        }
                    }
                    .toList()
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
                                    rate[toLowerCase(Locale.ROOT)]?.toDouble()
                                        ?: Double.InvalidValue
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
                    tokenPrice =
                        rateStorage.getRate(generateTokenHash(token.chainId, token.address))
                }
            }
        }
    }

    private fun getTokensForAccount(account: Account): Single<List<ERCToken>> =
        Single.zip(getTokensList(account), getTokenTransactions(account), BiFunction { tokens, tokensTx ->
            mutableListOf<ERCToken>().apply {
                tokens.forEach { token ->
                    if (token.type.isERC721()) {
                        tokensTx.tokens
                            .filter { tokenTx -> tokenTx.address == token.address }
                            .forEach { tx -> add(token.copy(tokenId = tx.tokenId)) }
                    } else {
                        add(token)
                    }
                }
            }
        })

    private fun getTokenTransactions(account: Account) =
        cryptoApi.getTokenTx(url = getTokensTxApiURL(account))


    private fun getTokensList(account: Account): Single<List<ERCToken>> =
        cryptoApi.getConnectedTokens(url = getTokensApiURL(account))
            .map { response ->
                mutableListOf<ERCToken>().apply {
                    response.tokens
                        .map { tokenData -> TokenDataToERCToken.map(account.chainId, tokenData, account.address) }
                        .filter { tokenData -> tokenData.type != TokenType.INVALID }
                        .forEach { token -> add(token) }
                }
            }

    private fun getTokensFromTx(account: Account): Single<List<ERCToken>> =
        Single.zip(getERC20TokensFromTx(account), getERC721TokensFromTx(account), BiFunction { erc20Tokens, erc721Tokens ->
            erc20Tokens + erc721Tokens
        })


    private fun getERC20TokensFromTx(account: Account): Single<List<ERCToken>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map { response ->
                mutableListOf<ERCToken>().apply {
                    response.tokens.forEach { tokenTx ->
                        add(ERCToken(account.chainId, tokenTx, account.address, TokenType.ERC20))
                    }
                }
            }

    private fun getERC721TokensFromTx(account: Account): Single<List<ERCToken>> =
        cryptoApi.getTokenTx(url = getTokenNftTxApiURL(account))
            .map { response ->
                mutableListOf<ERCToken>().apply {
                    response.tokens.forEach { tokenTx ->
                        add(ERCToken(account.chainId, tokenTx, account.address, TokenType.ERC721))
                    }
                }
            }

    private fun getTokenTxApiURL(account: Account): String =
        String.format(
            ETHEREUM_TOKENTX_REQUEST,
            getTokenExplorerURL(account.chainId),
            ERC20_TX_ACTION,
            account.address,
            getAPIKey(account.chainId)
        )

    private fun getTokenNftTxApiURL(account: Account): String =
        String.format(
            ETHEREUM_TOKENTX_REQUEST,
            getTokenExplorerURL(account.chainId),
            ERC721_TX_ACTION,
            account.address,
            getAPIKey(account.chainId)
        )

    @VisibleForTesting
    fun getTokensApiURL(account: Account): String =
        String.format(TOKEN_BALANCE_REQUEST, getTokenExplorerURL(account.chainId), account.address)

    @VisibleForTesting
    fun getTokensTxApiURL(account: Account): String =
        String.format(TOKEN_TX_REQUEST, getTokenExplorerURL(account.chainId), account.address)

    private fun getAPIKey(chainId: Int) =
        when (chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> ETHERSCAN_KEY
            MATIC -> POLYGONSCAN_KEY
            BSC, BSC_TESTNET -> BSCSCAN_KEY
            else -> throw NetworkNotFoundThrowable()
        }

    private fun getTokenExplorerURL(chainId: Int) =
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
            MATIC -> POLYGON_TOKEN_BALANCE_URL
            BSC -> BINANCE_SMART_CHAIN_MAINNET_TOKEN_BALANCE_URL
            BSC_TESTNET -> BINANCE_SMART_CHAIN_TESTNET_TOKEN_BALANCE_URL
            else -> throw NetworkNotFoundThrowable()
        }

    /**
     * arguments: tokens MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    @VisibleForTesting
    fun updateTokens(
        chainId: Int,
        newToken: ERCToken,
        tokens: MutableMap<Int, List<ERCToken>>
    ): Map<Int, List<ERCToken>> =
        tokens.apply {
            (this[chainId] ?: listOf()).toMutableList().let { currentTokens ->
                currentTokens.add(newToken)
                put(chainId, currentTokens)
                rateStorage.areRatesSynced = false
            }
        }

    override fun sortTokensByChainId(tokenList: List<ERCToken>): Map<Int, List<ERCToken>> =
        mutableMapOf<Int, MutableList<ERCToken>>().apply {
            tokenList.forEach { token ->
                get(token.chainId)?.add(token)
                    .orElse { put(token.chainId, mutableListOf(token)) }
            }
        }

    override fun mergeWithLocalTokensList(newTokensPerChainIdMap: Map<Int, List<ERCToken>>): UpdateTokensResult =
        walletManager.getWalletConfig().erc20Tokens.let { allLocalTokens ->
            var shouldUpdateLogosURI = false
            val allLocalTokensMap = allLocalTokens.toMutableMap()
            for ((chainId, newTokens) in newTokensPerChainIdMap) {
                val localChainTokens = allLocalTokensMap[chainId] ?: listOf()
                mergeNewTokensWithLocal(localChainTokens, newTokens)
                    .let { tokenList ->
                        if (!shouldUpdateLogosURI) {
                            shouldUpdateLogosURI = localChainTokens.size != tokenList.size
                        }
                        shouldUpdateLogosURI = true
                        allLocalTokensMap[chainId] = tokenList
                    }
            }
            UpdateTokensResult(shouldUpdateLogosURI, allLocalTokensMap)
        }

    private fun mergeNewTokensWithLocal(localChainTokens: List<ERCToken>, newTokens: List<ERCToken>) =
        mutableListOf<ERCToken>().apply {
            addAll(localChainTokens)
            newTokens.forEach { newToken ->
                if (isNewToken(newToken)) {
                    add(newToken)
                } else if (isNewTokenForAccount(newToken)) {
                    add(newToken)
                }
            }
        }

    private fun MutableList<ERCToken>.isNewTokenForAccount(newToken: ERCToken) =
        find { localToken ->
            localToken.address.equals(newToken.address, true) &&
                    localToken.accountAddress.equals(newToken.accountAddress, true)
        } == null

    private fun MutableList<ERCToken>.isNewToken(newToken: ERCToken) =
        if (newToken.type.isERC721()) {
            find { localToken ->
                localToken.address.equals(
                    newToken.address,
                    true
                ) && localToken.tokenId == newToken.tokenId
            } == null
        } else {
            find { localToken -> localToken.address.equals(newToken.address, true) } == null
        }

    override fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<UpdateTokensResult> =
        if (shouldBeUpdated) {
            getTokenIconsURL().map { logoUrls ->
                tokensPerChainIdMap.values.forEach { tokens ->
                    tokens.forEach { token ->
                        token.apply {
                            if (this.type.isERC20()) {
                                logoUrls[generateTokenHash(chainId, address)]?.let { newLogoURI ->
                                    logoURI = newLogoURI
                                }
                            }
                        }
                    }
                }
                UpdateTokensResult(true, tokensPerChainIdMap)
            }
        } else Single.just(UpdateTokensResult(false, tokensPerChainIdMap))

    override fun updateMissingNFTTokensDetails(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>,
        accounts: List<Account>
    ): Single<UpdateTokensResult> {
        val updatedTokensSingleList = getMissingNFTTokensDetails(tokensPerChainIdMap, accounts)
        return if (updatedTokensSingleList.isEmpty()) {
            Single.just(UpdateTokensResult(shouldBeUpdated, tokensPerChainIdMap))
        } else {
            Single.mergeDelayError(updatedTokensSingleList)
                .reduce(UpdateTokensResult(true, tokensPerChainIdMap)) { resultData, token ->
                    resultData.apply {
                        tokensPerChainIdMap[token.chainId]?.find {
                            it.address.equals(token.address, true) && it.accountAddress.equals(
                                token.accountAddress,
                                true
                            ) && token.tokenId == it.tokenId
                        }?.apply {
                            description = token.description
                            contentUri = token.contentUri
                            name = token.name
                        }
                    }
                }
        }
    }

    private fun getMissingNFTTokensDetails(
        tokensPerChainIdMap: Map<Int, List<ERCToken>>,
        accounts: List<Account>
    ): List<Single<ERCToken>> =
        mutableListOf<Single<ERCToken>>().apply {
            tokensPerChainIdMap.filterValues { tokens -> tokens.find { it.shouldNftDetailsBeUpdated() } != null }.values.forEach { tokens ->
                tokens.forEach { token ->
                    if (token.shouldNftDetailsBeUpdated()) {
                        val privateKey =
                            accounts.find { account -> account.chainId == token.chainId && account.address == token.accountAddress }?.privateKey
                        if (!privateKey.isNullOrBlank()) {
                            add(
                                token.updateMissingNFTTokensDetails(
                                    privateKey, token.chainId, token.address, BigInteger(token.tokenId!!)
                                )
                            )
                        }
                    }
                }
            }
        }

    private fun ERCToken.shouldNftDetailsBeUpdated() =
        type.isERC721() && (contentUri.isBlank() || collectionName.isNullOrBlank() || name.isBlank())

    private fun ERCToken.updateMissingNFTTokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<ERCToken> =
        erc721TokenRepository.getERC721DetailsUri(privateKey, chainId, tokenAddress, tokenId)
            .flatMap { url ->
                cryptoApi.getERC721TokenDetails(url).map { details ->
                    contentUri = details.contentUri
                    description = details.description
                    name = details.name
                    this
                }
            }

    /**
     *
     * arguments: map - downloaded tokens - Map<AccountPrivateKey, List<AccountToken>>, tokens - MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    @VisibleForTesting
    fun updateTokens(newTokensWithIcons: Map<Int, List<ERCToken>>): Map<Int, List<ERCToken>> {
        return mutableMapOf<Int, List<ERCToken>>().apply {
            for ((chainId, tokens) in newTokensWithIcons) {
                tokens.onEach { token ->
                    token.accountAddress = token.accountAddress.toLowerCase(Locale.ROOT)
                }
                this[chainId] = tokens.distinct()
            }
            rateStorage.areRatesSynced = false
        }
    }

    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val TOKEN_BALANCE_REQUEST = "%sapi?module=account&action=tokenlist&address=%s"
        private const val TOKEN_TX_REQUEST = "%sapi?module=account&action=tokentx&address=%s"
        private const val ETHEREUM_TOKENTX_REQUEST =
            "%sapi?module=account&action=%s&address=%s&startblock=0&endblock=999999999&sort=asc&apikey=%s"
        private const val ERC20_TX_ACTION = "tokentx"
        private const val ERC721_TX_ACTION = "tokennfttx"
        private const val TOKEN_ADDRESS_SEPARATOR = ","
    }
}