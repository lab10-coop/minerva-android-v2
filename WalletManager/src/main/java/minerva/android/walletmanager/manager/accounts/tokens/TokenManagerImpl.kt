package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.NftDetails
import minerva.android.apiProvider.model.TokenTx
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.superToken.SuperTokenRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.BuildConfig.*
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.exception.NotERC1155Throwable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_ONE
import minerva.android.walletmanager.model.defs.ChainId.Companion.ARB_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_C
import minerva.android.walletmanager.model.defs.ChainId.Companion.AVA_FUJ
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC
import minerva.android.walletmanager.model.defs.ChainId.Companion.BSC_TESTNET
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO_ALF
import minerva.android.walletmanager.model.defs.ChainId.Companion.CELO_BAK
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_SEP
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.ChainId.Companion.LUKSO_16
import minerva.android.walletmanager.model.defs.ChainId.Companion.MATIC
import minerva.android.walletmanager.model.defs.ChainId.Companion.MUMBAI
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_KOV
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_GOR
import minerva.android.walletmanager.model.defs.ChainId.Companion.OPT_BED
import minerva.android.walletmanager.model.defs.ChainId.Companion.ZKS_ALPHA
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.ChainId.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_MAIN
import minerva.android.walletmanager.model.defs.ChainId.Companion.RSK_TEST
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO
import minerva.android.walletmanager.model.defs.ChainId.Companion.GNO_CHAI
import minerva.android.walletmanager.model.mappers.TokenDataToERCToken
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
    private val rateStorage: RateStorage
) : TokenManager {
    private val currentTimeProvider = CurrentTimeProviderImpl()
    private var currentFiat = String.Empty

    @VisibleForTesting
    fun getTokenVisibility(accountAddress: String, tokenAddress: String) =
        localStorage.getTokenVisibilitySettings().getTokenVisibility(accountAddress, tokenAddress)


    override fun saveToken(accountAddress: String, chainId: Int, newToken: ERCToken): Completable =
        walletManager.getWalletConfig().erc20Tokens[chainId]
            .let { tokens ->
                var tag = String.Empty
                var type = TokenType.INVALID
                tokens?.find { token -> token.address.equals(newToken.address, true) }
                    ?.let { tag = it.tag }
                tokens?.find { token -> token.address.equals(newToken.address, true) }
                    ?.let { type = it.type }
                walletManager.getWalletConfig().run {
                    copy(
                        version = updateVersion,
                        erc20Tokens = updateTokens(
                            chainId,
                            newToken.copy(accountAddress = accountAddress, tag = tag, type = type),
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
        checkMissingNFTDetails()

    private fun checkMissingNFTDetails(): Completable =
        getNftCollectionDetails()
            .flatMapCompletable { tokenDetailsMap -> updateNFTsIcons(tokenDetailsMap) }

    private fun updateNFTsIcons(tokens: Map<String, NftCollectionDetailsResult>): Completable =
        walletManager.getWalletConfig().run {
            erc20Tokens.forEach { (id, tokenList) ->
                mergeNFTDetails(tokenList, tokens)
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

    private fun getSuperTokensPerAccount(account: Account): List<ERCToken> =
        getAllTokensPerAccount(account)
            .filter { isSuperToken(it.type, account) }

    override fun getSuperTokenBalance(account: Account): Flowable<Asset> {
        with(account) {
            val supertokensPerAccount = getSuperTokensPerAccount(this)
            return Flowable.mergeDelayError(
                getSuperTokenBalanceFlowables(
                    supertokensPerAccount,
                    this
                )
            )
                .flatMap { superTokenBalance ->
                    getSuperTokenNetFlow(superTokenBalance, account)
                        .map { consNetFlow ->
                            handleTokensBalances(
                                superTokenBalance,
                                getSuperTokensForAccount(supertokensPerAccount),
                                account,
                                consNetFlow
                            )
                        }
                }
        }
    }

    private fun handleTokensBalances(
        token: Token,
        tokens: List<ERCToken>,
        account: Account,
        constNetFlow: BigInteger? = null
    ): Asset =
        when (token) {
            is TokenWithBalance -> getAssetBalance(tokens, token, account, constNetFlow)
            else -> TokenToAssetBalanceErrorMapper.map(account, token as TokenWithError)
        }

    private fun Account.getSuperTokensForAccount(
        tokensPerAccount: List<ERCToken>
    ): List<ERCToken> =
        getTokensForAccount(tokensPerAccount, this)
            .filter { token -> token.type.isSuperToken() }

    private fun getSuperTokenBalanceFlowables(
        tokens: List<ERCToken>,
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
            val tokensPerAccount = getAllTokensPerAccount(this)
            val tokens = getTokensForAccount(tokensPerAccount, this)
            return Flowable.mergeDelayError(getTokenBalanceFlowables(tokens, this))
                // todo: can this be simplified?
                .flatMap { (token, type) ->
                    Flowable.just(token)
                }
                .map { token -> handleTokensBalances(token, tokens, account, null) }
        }
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

    private fun isSuperToken(type: TokenType, account: Account) =
        type.isSuperToken() && account.hasSuperfluidSupport()

    private fun String?.isEqualOrBothAreNullOrBlank(other: String?): Boolean {
        val areEqual = this == other
        val areBothNullOrBlank = (this.isNullOrBlank() && other.isNullOrBlank())
        return areEqual || areBothNullOrBlank
    }

    private fun getAssetBalance(
        tokens: List<ERCToken>,
        tokenWithBalance: TokenWithBalance,
        account: Account,
        consNetFlow: BigInteger?
    ): AssetBalance =
        tokens.find { token ->
            token.address.equals(tokenWithBalance.address, true)
                    && token.tokenId.isEqualOrBothAreNullOrBlank(tokenWithBalance.tokenId)
        }
            ?.let { token ->
                var tokenCopy = token.copy()
                if (consNetFlow != null) {
                    tokenCopy = tokenCopy.copy(consNetFlow = consNetFlow)
                }
                AssetBalance(
                    account.chainId,
                    account.privateKey,
                    getAccountToken(
                        tokenCopy,
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
    ): List<Flowable<Pair<Token, TokenType>>> =
        with(account) {
            val tokenBalanceFlowables = mutableListOf<Flowable<Pair<Token, TokenType>>>()
            tokens.forEach { ercToken ->
                if (ercToken.type.isERC721()) {
                    tokenBalanceFlowables.add(
                        erc721TokenRepository.getTokenBalance(
                            ercToken.tokenId ?: String.Empty,
                            privateKey,
                            chainId,
                            ercToken.address,
                            address
                        )
                            .map { token -> Pair(token, ercToken.type) }
                            .subscribeOn(Schedulers.io())
                    )
                } else if (ercToken.type.isERC1155()) {
                    tokenBalanceFlowables.add(
                        erc1155TokenRepository.getTokenBalance(
                            ercToken.tokenId ?: String.Empty,
                            privateKey,
                            chainId,
                            ercToken.address,
                            address
                        )
                            .map { token -> Pair(token, ercToken.type) }
                            .subscribeOn(Schedulers.io())
                    )
                } else {
                    if (ercToken.decimals.isNotBlank()) {
                        tokenBalanceFlowables.add(
                            erc20TokenRepository.getTokenBalance(
                                privateKey,
                                chainId,
                                ercToken.address,
                                address
                            )
                                .map { token -> Pair(token, ercToken.type) }
                                .subscribeOn(Schedulers.io())
                        )
                    }
                }
            }
            return tokenBalanceFlowables
        }

    private fun getTokensForAccount(
        tokensPerAccount: List<ERCToken>,
        account: Account
    ): List<ERCToken> =
        tokensPerAccount
            .filter { token -> token.chainId == account.chainId }

    private fun getAccountToken(ercToken: ERCToken, balance: BigDecimal): AccountToken =
        AccountToken(
            ercToken,
            balance,
            rateStorage.getRate(generateTokenHash(ercToken.chainId, ercToken.address))
        )

    override fun getTokensUpdate(): Flowable<List<ERCToken>> =
        Flowable.just(walletManager.getWalletConfig().erc20Tokens
            .flatMap { (id, tokenList) -> tokenList })

    override fun getSingleTokenRate(tokenHash: String): Double = rateStorage.getRate(tokenHash)

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
            ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR, ETH_SEP, GNO_CHAI, BSC_TESTNET -> getTokensFromTx(account)
            MUMBAI, LUKSO_16, RSK_TEST, RSK_MAIN, ARB_RIN, OPT_KOV, OPT_GOR, OPT_BED, ZKS_ALPHA, CELO_BAK, CELO_ALF, AVA_FUJ -> Single.just(emptyList()) // Networks without token explorer urls
            GNO, MATIC, ATS_SIGMA, BSC, ETH_MAIN, ARB_ONE, OPT, CELO, AVA_C -> getTokensOwned(account)
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
                                        chunkedTokens.map { it.address.toLowerCase(Locale.ROOT) }
                                            .toMutableList()
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
                            (contractAddress.toLowerCase(Locale.ROOT)).let { contractAddressLowered ->
                                add(
                                    Pair(
                                        generateTokenHash(chainId, contractAddressLowered),
                                        rate[currentFiat.toLowerCase(Locale.ROOT)]?.toDoubleOrNull()
                                            ?: Double.InvalidValue
                                    )
                                )
                                contractAddressesList.remove(
                                    contractAddressLowered.toLowerCase(
                                        Locale.ROOT
                                    )
                                )
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
                        .map { tokenData ->
                            TokensOwnedToERCToken.map(
                                account.chainId,
                                tokenData,
                                account.address
                            )
                        }
                        .filter { token -> token.type != TokenType.INVALID }
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
                                    .filter { (tokenTx, isTokenOwner) ->
                                        tokenTx.address.equals(
                                            token.address,
                                            true
                                        )
                                    }
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
                        .filter { tokenData -> tokenData.name != null }
                        .map { tokenData ->
                            TokenDataToERCToken.map(
                                account.chainId,
                                tokenData,
                                account.address
                            ) to tokenData.balance
                        }
                        .filter { (token, balance) -> token.type != TokenType.INVALID }
                        .forEach { (token, balance) -> add(token to balance) }
                }
            }

    private fun getTokensFromTx(account: Account): Single<List<ERCToken>> =
        Single.zip(
            getERC20TokensFromTx(account),
            getERC721TokensFromTx(account),
            BiFunction { erc20Tokens, erc721Tokens ->
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
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR, ETH_SEP -> ETHERSCAN_KEY
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
            ETH_SEP -> ETHEREUM_SEPOLIA_TOKEN_BALANCE_URL
            ATS_TAU -> ARTIS_TAU_TOKEN_BALANCE_URL
            ATS_SIGMA -> ARTIS_SIGMA_TOKEN_BALANCE_URL
            POA_SKL -> POA_SOKOL_TOKEN_BALANCE_URL
            POA_CORE -> POA_CORE_TOKEN_BALANCE_URL
            GNO -> GNO_TOKEN_BALANCE_URL
            GNO_CHAI -> GNO_CHAI_TOKEN_BALANCE_URL
            LUKSO_14 -> LUKSO_TOKEN_BALANCE_URL
            MATIC -> POLYGON_TOKEN_BALANCE_URL
            BSC -> BINANCE_SMART_CHAIN_MAINNET_TOKEN_BALANCE_URL
            BSC_TESTNET -> BINANCE_SMART_CHAIN_TESTNET_TOKEN_BALANCE_URL
            else -> throw NetworkNotFoundThrowable()
        }

    @VisibleForTesting
    fun getTokensOwnedURL(chainId: Int) =
        when (chainId) {
            GNO -> GNO_TOKENS_OWNED_URL
            MATIC -> POLYGON_TOKENS_OWNED_URL
            ATS_SIGMA -> ARTIS_SIGMA_TOKENS_OWNED_URL
            BSC -> BSC_TOKENS_OWNED_URL
            ETH_MAIN -> ETH_TOKENS_OWNED_URL
            ARB_ONE -> ARB_TOKENS_OWNED_URL
            OPT -> OPT_TOKENS_OWNED_URL
            CELO -> CELO_TOKENS_OWNED_URL
            AVA_C -> AVA_TOKENS_OWNED_URL
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
            val allLocalTokensMap = allLocalTokens.toMutableMap()
            for ((chainId, newTokens) in newTokensPerChainIdMap) {
                val localChainTokens = allLocalTokensMap[chainId] ?: listOf()
                mergeNewTokensWithLocal(localChainTokens, newTokens)
                    .let { tokenList ->
                        allLocalTokensMap[chainId] = tokenList
                    }
            }
            UpdateTokensResult(true, allLocalTokensMap)
        }

    private fun mergeNewTokensWithLocal(
        localChainTokens: List<ERCToken>,
        newTokens: List<ERCToken>
    ) =
        mutableListOf<ERCToken>().apply {
            addAll(localChainTokens)
            newTokens.forEach { newToken ->
                mergeNewTokenWithLocal(localChainTokens, newToken)
                if (isNewToken(newToken)) {
                    add(newToken)
                } else if (isNewTokenForAccount(newToken)) {
                    add(newToken)
                }
            }
        }

    private fun mergeNewTokenWithLocal(localChainTokens: List<ERCToken>, newToken: ERCToken) {
        localChainTokens.find { localToken ->
            localToken.address.equals(
                newToken.address,
                true
            )
        }?.apply {
            mergeLogoURI(newToken)
            if (newToken.type.isSuperToken()) {
                mergeSuperTokenDetailsAfterTokenDiscovery(newToken)
            }
        }
        if (newToken.type.isNft()) {
            localChainTokens.find { localToken ->
                localToken.address.equals(
                    newToken.address,
                    true
                ) && localToken.tokenId == newToken.tokenId && localToken.tokenId != null
            }?.apply {
                mergeNftDetailsAfterTokenDiscovery(newToken)
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

    override fun mergeNFTDetailsWithRemoteConfig(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<UpdateTokensResult> =
        getNftCollectionDetails().map { nftDetailsMap ->
            var shouldUpdate = shouldBeUpdated
            tokensPerChainIdMap.values.forEach { tokens ->
                mergeNFTDetails(tokens, nftDetailsMap) { shouldUpdate = true }
            }
            UpdateTokensResult(shouldUpdate, tokensPerChainIdMap)
        }

    private fun mergeNFTDetails(tokens : List<ERCToken>, nftDetailsMap: Map<String, NftCollectionDetailsResult>, onEachMerge: () -> Unit = {}) {
        tokens.forEach { token ->
            token.apply {
                nftDetailsMap[generateTokenHash(chainId, address)]?.let { nftDetails ->
                    logoURI = nftDetails.logoURI
                    if (nftDetails.override) {
                        collectionName = nftDetails.name
                        symbol = nftDetails.symbol
                    }
                    onEachMerge()
                }
            }
        }
    }

    private fun getActiveAccounts(): List<Account> =
        walletManager.getWalletConfig()
            .accounts.filter { account -> accountsFilter(account) && account.network.isAvailable() }

    private fun accountsFilter(account: Account) =
        refreshBalanceFilter(account) && account.network.testNet == !localStorage.areMainNetworksEnabled

    private fun refreshBalanceFilter(account: Account) =
        !account.isHide && !account.isDeleted && !account.isPending

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
                            it.nftContent = token.nftContent
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
                            token.tokenId?.let { tokenId ->
                                when (token.type) {
                                    TokenType.ERC1155 -> {
                                        add(
                                            token.updateMissingERC1155TokensDetails(
                                                privateKey,
                                                token.chainId,
                                                token.address,
                                                BigInteger(tokenId),
                                                token.nftContent.tokenUri
                                            )
                                        )
                                    }
                                    TokenType.ERC721 -> {
                                        add(
                                            token.updateMissingERC721TokensDetails(
                                                privateKey,
                                                token.chainId,
                                                token.address,
                                                BigInteger(tokenId),
                                                token.nftContent.tokenUri
                                            )
                                        )
                                    }
                                    else -> {
                                        // Do nothing
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun ERCToken.shouldNftDetailsBeUpdated() =
        type.isNft() && (nftContent.imageUri.isBlank() || collectionName.isNullOrBlank() || name.isBlank() || nftContent.description.isBlank())

    override fun updateMissingERC721TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String,
        token: ERCToken
    ): Single<ERCToken> = token.updateMissingERC721TokensDetails(privateKey, chainId, tokenAddress, tokenId, tokenUri)

    override fun updateMissingERC1155TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String,
        token: ERCToken
    ): Single<ERCToken> = token.updateMissingERC1155TokensDetails(privateKey, chainId, tokenAddress, tokenId, tokenUri)

    private fun ERCToken.updateMissingERC721TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String
    ): Single<ERCToken> = if (tokenUri.isEmpty())
        erc721TokenRepository.getERC721DetailsUri(privateKey, chainId, tokenAddress, tokenId)
            .flatMap { url ->
                getERC721TokenDetails(url)
            }
    else getERC721TokenDetails(tokenUri)

    private fun ERCToken.getERC721TokenDetails(url: String) =
        cryptoApi.getERC721TokenDetails(parseIPFSContentUrl(url)).map { details ->
            handleContentType(details)
            this
        }

    private fun ERCToken.updateMissingERC1155TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String
    ): Single<ERCToken> =
        if (tokenUri.isEmpty())
            erc1155TokenRepository.getERC1155DetailsUri(privateKey, chainId, tokenAddress, tokenId)
                .flatMap { url ->
                    getERC1155TokenDetails(url)
                }
        else
            getERC1155TokenDetails(tokenUri)

    private fun ERCToken.getERC1155TokenDetails(url: String) =
        cryptoApi.getERC1155TokenDetails(parseIPFSContentUrl(url)).map { details ->
            handleContentType(details)
            this
        }

    private fun ERCToken.handleContentType(details: NftDetails) {
        val type = when {
            !details.animationUrl.isNullOrBlank() -> ContentType.VIDEO
            else -> ContentType.IMAGE
        }
        nftContent = NftContent(
            parseIPFSContentUrl(details.imageUri),
            type,
            parseIPFSContentUrl(details.animationUrl ?: String.Empty),
            background = details.background ?: String.Empty,
            description = details.description
        )
        name = details.name
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

    override fun getERC721TokenDetails(privateKey: String, chainId: Int, tokenAddress: String): Single<ERCToken> =
        erc721TokenRepository.run {
            Observable.zip(
                getERC721TokenName(privateKey, chainId, tokenAddress),
                getERC721TokenSymbol(privateKey, chainId, tokenAddress),
                BiFunction<String, String, ERCToken> { name, symbol ->
                    ERCToken(
                        chainId = chainId,
                        collectionName = name,
                        symbol = symbol,
                        address = tokenAddress,
                        type = TokenType.ERC721
                    )
                }
            ).firstOrError()
        }

    override fun getERC1155TokenDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String
    ): Single<ERCToken> =
        erc1155TokenRepository.isERC1155(privateKey, chainId, tokenAddress)
            .map {
                if (it) {
                    ERCToken(
                        chainId = chainId,
                        address = tokenAddress,
                        type = TokenType.ERC1155
                    )
                } else {
                    throw NotERC1155Throwable()
                }
            }


    override fun isNftOwner(
        type: TokenType,
        tokenId: String,
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        ownerAddress: String
    ): Single<Boolean> = when (type) {
        TokenType.ERC721 -> erc721TokenRepository.isTokenOwner(
            tokenId,
            privateKey,
            chainId,
            tokenAddress,
            ownerAddress
        )
        TokenType.ERC1155 -> erc1155TokenRepository.getTokenBalance(
            tokenId,
            privateKey,
            chainId,
            tokenAddress,
            ownerAddress
        )
            .firstOrError()
            .flatMap {
                if (it is TokenWithBalance) {
                    Single.just(it.balance > BigDecimal.ZERO)
                } else Single.just(false)
            }
        else -> Single.just(false)
    }


    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val TOKENS_OWNED_REQUEST = "%stokensowned/%s?fetchTokenJson=all"
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