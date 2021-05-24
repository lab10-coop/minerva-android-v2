package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
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
import minerva.android.kotlinUtils.list.removeAll
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
import java.math.BigDecimal
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

    override fun loadCurrentTokens(chainId: Int): List<ERC20Token> =
        walletManager.getWalletConfig().run {
            NetworkManager.getTokens(chainId).mergeWithoutDuplicates(erc20Tokens[chainId] ?: listOf())
        }

    override fun saveToken(chainId: Int, token: ERC20Token): Completable =
        walletManager.getWalletConfig().run {
            copy(version = updateVersion, erc20Tokens = updateTokens(chainId, token, erc20Tokens.toMutableMap())).let {
                walletManager.updateWalletConfig(it)
            }
        }

    override fun saveTokens(shouldBeSaved: Boolean, map: Map<Int, List<ERC20Token>>): Single<Boolean> =
        if (shouldBeSaved) {
            walletManager.getWalletConfig().run {
                copy(version = updateVersion, erc20Tokens = updateTokens(map, erc20Tokens.toMutableMap())).let {
                    walletManager.updateWalletConfig(it)
                }.toSingle { shouldBeSaved }.onErrorReturn { shouldBeSaved }
            }
        } else Single.just(shouldBeSaved)

    override fun checkMissingTokensDetails(): Completable =
        cryptoApi.getLastCommitFromTokenList(url = ERC20_TOKEN_DATA_LAST_COMMIT)
            .zipWith(tokenDao.getTaggedTokens())
            .filter { (commits, tokens) -> isNewCommit(commits) || tokens.isEmpty() }
            .flatMapSingle { getMissingTokensDetails() }
            .flatMap { tokenDetailsMap ->
                updateTokensIcons(tokenDetailsMap)
                    .toSingleDefault(tokenDetailsMap.values.toList())
            }
            .map { tokenDetailsList ->
                tokenDetailsList.filter {
                    it.tags.isNotEmpty() && (it.tags.contains(TokenTag.SUPER_TOKEN.tag) ||
                            it.tags.contains(TokenTag.WRAPPER_TOKEN.tag))
                }
            }
            .map { taggedTokens ->
                localStorage.saveTokenIconsUpdateTimestamp(currentTimeProvider.currentTimeMills())
                tokenDao.updateTokens(TokenDetailsToERC20TokensMapper.map(taggedTokens))
            }.ignoreElement()

    private fun isNewCommit(list: List<CommitElement>): Boolean =
        list[LAST_UPDATE_INDEX].lastCommitDate.let {
            localStorage.loadTokenIconsUpdateTimestamp() < DateUtils.getTimestampFromDate(it)
        }

    private fun getMissingTokensDetails(): Single<Map<String, TokenDetails>> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL)
            .map { tokens -> tokens.associateBy { generateTokenHash(it.chainId, it.address) } }

    private fun updateTokensIcons(tokens: Map<String, TokenDetails>): Completable =
        walletManager.getWalletConfig().run {
            erc20Tokens.forEach { (key, value) ->
                value.forEach {
                    it.logoURI = tokens[generateTokenHash(key, it.address)]?.logoURI
                }
            }
            walletManager.updateWalletConfig(copy(version = updateVersion))
        }

    override fun getTokenIconURL(chainId: Int, address: String): Single<String> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.find { chainId == it.chainId && address == it.address }?.logoURI ?: String.Empty
        }

    //TODO add refreshing optimalization for tokens for current account, do: per account not per network
    override fun refreshTokensBalances(account: Account): Single<Pair<String, List<AccountToken>>> =
        tokenDao.getTaggedTokens()
            .zipWith(Single.just(loadCurrentTokens(account.chainId)))
            .flatMap { (taggedTokens, activeTokens) ->
                val tokens = taggedTokens.mergeWithoutDuplicates(activeTokens)
                    .filter { token -> token.chainId == account.chainId }
                Observable.fromIterable(tokens)
                    .flatMap {
                        blockchainRepository.refreshTokenBalance(
                            account.privateKey,
                            account.chainId,
                            it.address,
                            account.address
                        )
                    }
                    .map { (address, balance) ->
                        tokens.find { it.address == address }?.let { erc20Token ->
                            AccountToken(
                                erc20Token,
                                balance,
                                rateStorage.getRate(generateTokenHash(erc20Token.chainId, erc20Token.address))
                            )
                        }.orElse { throw NullPointerException() }
                    }
                    .toList()
                    .flatMap { accountTokens ->
                        makeTaggedTokensActive(accountTokens.toList(), activeTokens, account.chainId)
                    }
                    .map { accountTokens -> Pair(account.privateKey, accountTokens) }
            }

    private fun makeTaggedTokensActive(
        accountTokens: List<AccountToken>,
        activeTokens: List<ERC20Token>,
        chainId: Int
    ): Single<List<AccountToken>> {
        val taggedActiveTokens = mutableListOf<ERC20Token>()
        accountTokens
            .filter { accountToken -> accountToken.token.tag != String.Empty && accountToken.balance > BigDecimal.ZERO }
            .onEach { taggedActiveTokens.add(it.token) }

        return if (shouldMakeTaggedTokensActive(taggedActiveTokens, activeTokens)) {
            saveTokens(true, mapOf(chainId to taggedActiveTokens))
                .ignoreElement()
                .onErrorComplete()
                .toSingleDefault(accountTokens)

        } else {
            Single.just(accountTokens)
        }
    }

    private fun shouldMakeTaggedTokensActive(
        taggedActiveTokens: MutableList<ERC20Token>,
        activeTokens: List<ERC20Token>
    ) = taggedActiveTokens.isNotEmpty() && !activeTokens.containsAll(taggedActiveTokens)

    override fun sortTokensByChainId(tokenList: List<ERC20Token>): Map<Int, List<ERC20Token>> =
        mutableMapOf<Int, MutableList<ERC20Token>>().apply {
            tokenList.forEach { token ->
                get(token.chainId)?.add(token)
                    .orElse { put(token.chainId, mutableListOf(token)) }
            }
        }

    override fun mergeWithLocalTokensList(map: Map<Int, List<ERC20Token>>): Pair<Boolean, Map<Int, List<ERC20Token>>> =
        walletManager.getWalletConfig().erc20Tokens.let { allLocalTokens ->
            var updateLogosURI = false
            val updatedMap = allLocalTokens.toMutableMap()
            for ((chainId, tokens) in map) {
                val localChainTokens = updatedMap[chainId] ?: listOf()
                localChainTokens.mergeWithoutDuplicates(tokens).let {
                    updateLogosURI = localChainTokens.size != it.size
                    updatedMap[chainId] = it
                }
            }
            Pair(updateLogosURI, updatedMap)
        }

    override fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        accountTokens: Map<Int, List<ERC20Token>>
    ): Single<Pair<Boolean, Map<Int, List<ERC20Token>>>> =
        if (shouldBeUpdated) {
            getTokenIconsURL().map { logoUrls ->
                accountTokens.values.forEach { accountTokens ->
                    accountTokens.forEach {
                        it.apply {
                            logoUrls[generateTokenHash(chainId, address)]?.let { newLogoURI ->
                                logoURI = newLogoURI
                            }
                        }
                    }
                }
                Pair(true, accountTokens)
            }
        } else Single.just(Pair(false, accountTokens))

    override fun getSingleTokenRate(tokenHash: String): Double = rateStorage.getRate(tokenHash)

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenDetails(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.associate { generateTokenHash(it.chainId, it.address) to it.logoURI }
        }

    override fun downloadTokensList(account: Account): Single<List<ERC20Token>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> getEthereumTokens(account)
            else -> getNotEthereumTokens(account)
        }

    private fun prepareContractAddresses(tokens: List<ERC20Token>): String =
        tokens.joinToString(TOKEN_ADDRESS_SEPARATOR) { token -> token.address }

    override fun getTokensRate(tokens: Map<Int, List<ERC20Token>>): Completable =
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
                        .forEach { tokenData -> add(TokenDataToERC20Token.map(account.chainId, tokenData)) }
                }
            }

    private fun getEthereumTokens(account: Account): Single<List<ERC20Token>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map { response ->
                mutableListOf<ERC20Token>().apply {
                    response.tokens.forEach { tokenTx -> add(ERC20Token(account.chainId, tokenTx)) }
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
    fun updateTokens(chainId: Int, token: ERC20Token, tokens: MutableMap<Int, List<ERC20Token>>) =
        tokens.apply {
            (this[chainId] ?: listOf()).toMutableList().let { currentTokens ->
                currentTokens.removeAll { it.address == token.address }
                currentTokens.add(token)
                put(chainId, currentTokens)
                rateStorage.areRatesSynced = false
            }
        }

    /**
     *
     * arguments: map - downloaded tokens - Map<AccountPrivateKey, List<AccountToken>>, tokens - MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    @VisibleForTesting
    fun updateTokens(map: Map<Int, List<ERC20Token>>, tokens: MutableMap<Int, List<ERC20Token>>) =
        tokens.apply {
            map.values.forEach { newTokens ->
                newTokens.forEach { accountToken ->
                    (this[accountToken.chainId] ?: listOf()).toMutableList().let { currentTokens ->
                        currentTokens.removeAll { it.address == accountToken.address }
                        currentTokens.add(accountToken)
                        put(accountToken.chainId, currentTokens)
                        rateStorage.areRatesSynced = false
                    }
                }
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