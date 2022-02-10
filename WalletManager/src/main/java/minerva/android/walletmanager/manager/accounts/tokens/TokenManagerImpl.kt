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
import minerva.android.apiProvider.model.TokenTx
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
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
import minerva.android.walletmanager.model.mappers.TokensOwnedToERCToken
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.*
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.utils.MarketUtils
import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import minerva.android.walletmanager.utils.parseIPFSContentUrl
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
    private val erc1155TokenRepository: ERC1155TokenRepository,
    private val rateStorage: RateStorage,
    database: MinervaDatabase
) : TokenManager {
    override var activeSuperTokenStreams: MutableList<ActiveSuperToken> = mutableListOf()
    private val currentTimeProvider = CurrentTimeProviderImpl()
    private val tokenDao: TokenDao = database.tokenDao()
    private var currentFiat = String.Empty

    @VisibleForTesting
    fun getTokenVisibility(accountAddress: String, tokenAddress: String) =
        localStorage.getTokenVisibilitySettings().getTokenVisibility(accountAddress, tokenAddress)


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
            ) && token.type.isNft()
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

    private fun String?.isEqualOrBothAreNullOrBlank(other: String?): Boolean {
        val areEqual = this == other
        val areBothNullOrBlank = (this.isNullOrBlank() && other.isNullOrBlank())
        return areEqual || areBothNullOrBlank
    }

    private fun getAssetBalance(
        tokens: List<ERCToken>,
        tokenWithBalance: TokenWithBalance,
        account: Account
    ): AssetBalance =
        tokens.find { token ->  token.address.equals(tokenWithBalance.address, true)
                    && token.tokenId.isEqualOrBothAreNullOrBlank(tokenWithBalance.tokenId) }
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
            .orElse {
                throw NullPointerException()
            }

    private fun getTokenBalanceFlowables(
        tokens: List<ERCToken>,
        account: Account
    ): List<Flowable<Pair<Token, String>>> =
        with(account) {
            val tokenBalanceFlowables = mutableListOf<Flowable<Pair<Token, String>>>()
            tokens.forEach { ercToken ->
                if (ercToken.type.isERC721()) {
                    tokenBalanceFlowables.add(
                        erc721TokenRepository.getTokenBalance(ercToken.tokenId ?: String.Empty, privateKey, chainId, ercToken.address, address)
                            .map { token -> Pair(token, ercToken.tag) }
                            .subscribeOn(Schedulers.io())
                    )
                } else if (ercToken.type.isERC1155()) {
                    tokenBalanceFlowables.add(
                        erc1155TokenRepository.getTokenBalance(ercToken.tokenId ?: String.Empty, privateKey, chainId, ercToken.address, address)
                            .map { token -> Pair(token, ercToken.tag) }
                            .subscribeOn(Schedulers.io())
                    )
                }  else {
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

    private fun getNftCollectionDetails(): Single<Map<String, NftCollectionDetailsResult>> =
        cryptoApi.getNftCollectionDetails().map { data ->
            data.associate { details ->
                generateTokenHash(
                    details.chainId,
                    details.address
                ) to NftCollectionDetailsResult(
                    details.logoURI,
                    details.name,
                    details.symbol,
                    details.override
                )
            }
        }

    override fun downloadTokensList(account: Account): Single<List<ERCToken>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR, BSC_TESTNET -> getTokensFromTx(account)
            MUMBAI, RSK_TEST, RSK_MAIN -> Single.just(emptyList()) // Networks without token explorer urls
            XDAI, MATIC, ATS_SIGMA, BSC -> getTokensOwned(account)
            else -> getTokensForAccount(account)
        }

    private fun prepareContractAddresses(tokens: List<ERCToken>): String =
        tokens.joinToString(TOKEN_ADDRESS_SEPARATOR) { token -> token.address }

    private fun shouldUpdateRate(token: ERCToken) =
        rateStorage.shouldUpdateRate(generateTokenHash(token.chainId, token.address))
            .and(getTokenVisibility(token.accountAddress, token.address) ?: true)


    override fun getTokensRates(tokens: Map<Int, List<ERCToken>>): Completable =
        mutableListOf<Observable<List<Pair<String, Double>>>>().let { observables ->
            with(localStorage.loadCurrentFiat()) {
                tokens.forEach { (chainId, tokens) ->
                    val marketId = MarketUtils.getTokenGeckoMarketId(chainId)
                    if (marketId != String.Empty) {
                        tokens.distinctBy { it.address }
                            .filter { shouldUpdateRate(it) }
                            .chunked(TOKEN_LIMIT_PER_CALL)
                            .forEach { chunkedTokens ->
                                observables.add(
                                    updateAccountTokensRate(
                                        marketId,
                                        chainId,
                                        prepareContractAddresses(chunkedTokens),
                                        chunkedTokens.map { it.address.toLowerCase(Locale.ROOT) }.toMutableList()
                                    )
                                )
                            }
                    }
                }
                Observable.merge(observables)
                    .doOnNext { rates ->
                        rates.forEach { (rateHash, rate) ->
                            rateStorage.saveRate(rateHash, rate)
                        }
                    }
                    .toList()
                    .doOnSuccess {
                        currentFiat = this
                    }
                    .ignoreElement()
            }
        }

    private fun updateAccountTokensRate(
        marketId: String,
        chainId: Int,
        contractAddresses: String,
        contractAddressesList: MutableList<String>
    ): Observable<List<Pair<String, Double>>> =
        localStorage.loadCurrentFiat().let { currentFiat ->
            cryptoApi.getTokensRate(marketId, contractAddresses, currentFiat)
                .map { tokenRateResponse ->
                    mutableListOf<Pair<String, Double>>().apply {
                        tokenRateResponse.forEach { (contractAddress, rate) ->
                            (contractAddress.toLowerCase(Locale.ROOT)).let{ contractAddressLowered ->
                                add(
                                    Pair(
                                        generateTokenHash(chainId, contractAddressLowered),
                                        rate[currentFiat.toLowerCase(Locale.ROOT)]?.toDoubleOrNull()
                                            ?: Double.InvalidValue
                                    )
                                )
                                contractAddressesList.remove(contractAddressLowered.toLowerCase(Locale.ROOT))
                            }
                        }
                        contractAddressesList.forEach { contractAddress ->
                            add(
                                Pair(
                                    generateTokenHash(chainId, contractAddress),
                                    Double.InvalidValue
                                )
                            )
                        }

                    }.toList()
                }
                .toObservable()
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

    private fun getTokenTransactionsWithOwnership(account: Account) = getTokenTransactions(account)
        .flattenAsObservable { it.tokens }
        .flatMap { tokenTx ->
            Observable.zip(
                Observable.just(tokenTx),
                if (tokenTx.tokenId.isNotBlank()) {
                    erc721TokenRepository.isTokenOwner(
                        tokenTx.tokenId,
                        account.privateKey,
                        account.chainId,
                        tokenTx.address,
                        account.address
                    ).toObservable().onErrorReturn { true }
                } else Observable.just(false),
                BiFunction { token: TokenTx, isTokenOwner: Boolean ->
                    Pair<TokenTx, Boolean>(token, isTokenOwner)
                })
        }
        .toList()

    private fun getTokensOwned(account: Account): Single<List<ERCToken>> =
        cryptoApi.getTokensOwned(url = getTokensOwnedApiURL(account))
            .map { response ->
                mutableListOf<ERCToken>().apply {
                    response.result
                        .map { tokenData -> TokensOwnedToERCToken.map(account.chainId, tokenData, account.address) }
                        .filter { token-> token.type != TokenType.INVALID }
                        .forEach { token -> add(token) }
                }
            }


    private fun getTokensForAccount(account: Account): Single<List<ERCToken>> =
        Single.zip(getTokensListWithBalance(account),
            getTokenTransactionsWithOwnership(account),
            BiFunction { tokens, tokensTx ->
                mutableListOf<ERCToken>().apply {
                    tokens.forEach { (token, balance) ->
                        if (token.type.isNft()) {
                            mutableListOf<ERCToken>().also { collectionTokens ->
                                tokensTx
                                    .filter { (tokenTx, isTokenOwner) -> tokenTx.address.equals(token.address, true) }
                                    .distinctBy { (tokenTx, isTokenOwner) -> tokenTx.tokenId }
                                    .forEach { (tokenTx, isTokenOwner) ->
                                        if (isTokenOwner) {
                                            collectionTokens.add(token.copy(tokenId = tokenTx.tokenId))
                                        }
                                    }
                                if (token.type.isERC721()) {
                                    balance.toIntOrNull()?.let { balance ->
                                        repeat(balance - collectionTokens.size) {
                                            collectionTokens.add(token)
                                        }
                                    }
                                }
                                addAll(collectionTokens)
                            }
                        } else {
                            add(token)
                        }
                    }
                }
            })

    private fun getTokenTransactions(account: Account) =
        cryptoApi.getTokenTx(url = getTokensTxApiURL(account))


    private fun getTokensListWithBalance(account: Account): Single<List<Pair<ERCToken, String>>> =
        cryptoApi.getConnectedTokens(url = getTokensApiURL(account))
            .map { response ->
                mutableListOf<Pair<ERCToken, String>>().apply {
                    response.tokens
                        .filter { tokenData-> tokenData.name != null }
                        .map { tokenData -> TokenDataToERCToken.map(account.chainId, tokenData, account.address) to tokenData.balance }
                        .filter { (token, balance) -> token.type != TokenType.INVALID }
                        .forEach { (token, balance) -> add(token to balance) }
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

    private fun getTokensOwnedApiURL(account: Account): String =
        String.format(TOKENS_OWNED_REQUEST, getTokensOwnedURL(account.chainId), account.address)

    private fun getAPIKey(chainId: Int) =
        when (chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> ETHERSCAN_KEY
            MATIC -> POLYGONSCAN_KEY
            BSC, BSC_TESTNET -> BSCSCAN_KEY
            else -> throw NetworkNotFoundThrowable()
        }

    override fun hasTokenExplorer(chainId: Int): Boolean = try {
        getTokenExplorerURL(chainId)
        true
    } catch (e: NetworkNotFoundThrowable) {
        false
    }

    @VisibleForTesting
    fun getTokenExplorerURL(chainId: Int) =
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

    @VisibleForTesting
    fun getTokensOwnedURL(chainId: Int) =
        when (chainId) {
            XDAI -> X_DAI_TOKENS_OWNED_URL
            MATIC -> POLYGON_TOKENS_OWNED_URL
            ATS_SIGMA -> ARTIS_SIGMA_TOKENS_OWNED_URL
            BSC -> BSC_TOKENS_OWNED_URL
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
            val shouldUpdateLogosURI = true
            val allLocalTokensMap = allLocalTokens.toMutableMap()
            for ((chainId, newTokens) in newTokensPerChainIdMap) {
                val localChainTokens = allLocalTokensMap[chainId] ?: listOf()
                mergeNewTokensWithLocal(localChainTokens, newTokens)
                    .let { tokenList ->
                        allLocalTokensMap[chainId] = tokenList
                    }
            }
            UpdateTokensResult(shouldUpdateLogosURI, allLocalTokensMap)
        }

    private fun mergeNewTokensWithLocal(localChainTokens: List<ERCToken>, newTokens: List<ERCToken>) =
        mutableListOf<ERCToken>().apply {
            addAll(localChainTokens.filter { it.type.isERC20() })
            newTokens.forEach { newToken ->
                mergeNewTokenWithLocalNfts(localChainTokens, newToken)
                if (isNewToken(newToken)) {
                    add(newToken)
                } else if (isNewTokenForAccount(newToken)) {
                    add(newToken)
                }
            }
        }

    private fun mergeNewTokenWithLocalNfts(localChainTokens: List<ERCToken>, newToken: ERCToken) {
        if (newToken.type.isNft()) {
            localChainTokens.find { localToken ->
                localToken.address.equals(
                    newToken.address,
                    true
                ) && localToken.tokenId == newToken.tokenId && localToken.tokenId != null
            }?.apply {
                newToken.mergeNftDetails(this)
            }
        }
    }

    private fun MutableList<ERCToken>.isNewTokenForAccount(newToken: ERCToken) =
        if (newToken.type.isNft()) {
            find { localToken ->
                localToken.address.equals(newToken.address, true) &&
                        localToken.accountAddress.equals(newToken.accountAddress, true)
                        && localToken.tokenId == newToken.tokenId && localToken.tokenId != null
            } == null
        } else {
            find { localToken ->
                localToken.address.equals(newToken.address, true) &&
                        localToken.accountAddress.equals(newToken.accountAddress, true)
            } == null
        }

    private fun MutableList<ERCToken>.isNewToken(newToken: ERCToken) =
        if (newToken.type.isNft()) {
            find { localToken ->
                localToken.address.equals(
                    newToken.address,
                    true
                ) && localToken.tokenId == newToken.tokenId && localToken.tokenId != null
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

    override fun mergeNFTDetailsWithRemoteConfig(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<UpdateTokensResult> =
        getNftCollectionDetails().map { nftDetailsMap ->
            var shouldUpdate = shouldBeUpdated
            tokensPerChainIdMap.values.forEach { tokens ->
                tokens.forEach { token ->
                    token.apply {
                        nftDetailsMap[generateTokenHash(chainId, address)]?.let { nftDetails ->
                            logoURI = nftDetails.logoURI
                            shouldUpdate = true
                            if (nftDetails.override) {
                                collectionName = nftDetails.name
                                symbol = nftDetails.symbol
                            }
                        }
                    }
                }
            }
            UpdateTokensResult(shouldUpdate, tokensPerChainIdMap)
        }

    private fun getActiveAccounts(): List<Account> =
        walletManager.getWalletConfig()
            .accounts.filter { account -> accountsFilter(account) && account.network.isAvailable() }

    private fun accountsFilter(account: Account) =
        refreshBalanceFilter(account) && account.network.testNet == !localStorage.areMainNetworksEnabled

    private fun refreshBalanceFilter(account: Account) = !account.isHide && !account.isDeleted && !account.isPending

    override fun fetchNFTsDetails(): Single<Boolean> =
        walletManager.getWalletConfig().erc20Tokens.let { allLocalTokens ->
            updateMissingNFTTokensDetails(allLocalTokens.toMutableMap(), getActiveAccounts())
                .flatMap { (shouldUpdate, tokensPerChainIdMap) ->
                    mergeNFTDetailsWithRemoteConfig(shouldUpdate, tokensPerChainIdMap)
                }
                .flatMap { (shouldUpdate, tokensPerChainIdMap) ->
                    saveTokens(shouldUpdate, tokensPerChainIdMap)
                }
        }


    override fun updateMissingNFTTokensDetails(
        tokensPerChainIdMap: Map<Int, List<ERCToken>>,
        accounts: List<Account>
    ): Single<UpdateTokensResult> {
        val updatedTokensSingleList =
            fetchMissingNFTTokensDetails(tokensPerChainIdMap, accounts)
        return if (updatedTokensSingleList.isEmpty()) {
            Single.just(UpdateTokensResult(false, tokensPerChainIdMap))
        } else {
            Single.mergeDelayError(updatedTokensSingleList)
                .reduce(UpdateTokensResult(true, tokensPerChainIdMap)) { resultData, token ->
                    resultData.apply {
                        tokensPerChainIdMap[token.chainId]?.filter {
                            it.address.equals(token.address, true) && it.accountAddress.equals(
                                token.accountAddress,
                                true
                            ) && token.tokenId == it.tokenId
                        }?.forEach {
                            it.description = token.description
                            it.contentUri = token.contentUri
                            it.name = token.name
                        }
                    }
                }
                .onErrorReturn {
                    UpdateTokensResult(true, tokensPerChainIdMap)
                }
        }
    }

    private fun fetchMissingNFTTokensDetails(
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
                            token.tokenId?.let{ tokenId ->
                                when (token.type) {
                                    TokenType.ERC1155 -> {
                                        add(
                                            token.updateMissingERC1155TokensDetails(
                                                privateKey,
                                                token.chainId,
                                                token.address,
                                                BigInteger(tokenId)
                                            )
                                        )
                                    }
                                    TokenType.ERC721 -> {
                                        add(
                                            token.updateMissingERC721TokensDetails(
                                                privateKey,
                                                token.chainId,
                                                token.address,
                                                BigInteger(tokenId)
                                            )
                                        )
                                    }
                                    else -> {
                                        // Do nothing}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun ERCToken.shouldNftDetailsBeUpdated() =
        type.isNft() && (contentUri.isBlank() || collectionName.isNullOrBlank() || name.isBlank() || description.isBlank())

    private fun ERCToken.updateMissingERC721TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<ERCToken> =
        erc721TokenRepository.getERC721DetailsUri(privateKey, chainId, tokenAddress, tokenId)
            .flatMap { url ->
                cryptoApi.getERC721TokenDetails(parseIPFSContentUrl(url)).map { details ->
                    contentUri = parseIPFSContentUrl(details.contentUri)
                    description = details.description
                    name = details.name
                    this
                }
            }

    private fun ERCToken.updateMissingERC1155TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<ERCToken> =
        erc1155TokenRepository.getERC1155DetailsUri(privateKey, chainId, tokenAddress, tokenId)
            .flatMap { url ->
                cryptoApi.getERC1155TokenDetails(parseIPFSContentUrl(url)).map { details ->
                    contentUri = parseIPFSContentUrl(details.contentUri)
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
        }
    }

    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val TOKENS_OWNED_REQUEST = "%stokensowned/%s?fetchTokenJson=ERC-1155"
        private const val TOKEN_BALANCE_REQUEST = "%sapi?module=account&action=tokenlist&address=%s"
        private const val TOKEN_TX_REQUEST = "%sapi?module=account&action=tokentx&address=%s"
        private const val ETHEREUM_TOKENTX_REQUEST =
            "%sapi?module=account&action=%s&address=%s&startblock=0&endblock=999999999&sort=asc&apikey=%s"
        private const val ERC20_TX_ACTION = "tokentx"
        private const val ERC721_TX_ACTION = "tokennfttx"
        private const val TOKEN_ADDRESS_SEPARATOR = ","
        private const val TOKEN_LIMIT_PER_CALL = 25
    }
}