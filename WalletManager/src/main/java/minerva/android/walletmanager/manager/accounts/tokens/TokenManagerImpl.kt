package minerva.android.walletmanager.manager.accounts.tokens

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.MarketData
import minerva.android.apiProvider.model.FiatPrice
import minerva.android.apiProvider.model.TokenMarketResponse
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.kotlinUtils.list.removeAll
import minerva.android.walletmanager.BuildConfig.*
import minerva.android.walletmanager.exception.AllTokenIconsUpdated
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
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.TempStorage
import minerva.android.walletmanager.utils.MarketUtils
import java.util.*

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val tempStorage: TempStorage
) : TokenManager {

    private val currentTimeProvider = CurrentTimeProviderImpl()
    private var currentFiat = String.Empty

    override fun loadCurrentTokens(chainId: Int): List<ERC20Token> = walletManager.getWalletConfig().run {
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
                }
                    .toSingle { shouldBeSaved }
                    .onErrorReturn { shouldBeSaved }
            }
        } else Single.just(shouldBeSaved)

    override fun updateTokenIcons(): Completable =
        cryptoApi.getTokenLastCommitRawData(url = ERC20_TOKEN_DATA_LAST_COMMIT)
            .flatMap {
                if (checkUpdates(it)) getTokenIconsURL()
                else Single.error(AllTokenIconsUpdated())
            }
            .flatMapCompletable { updateAllTokenIcons(it) }
            .doOnComplete { localStorage.saveTokenIconsUpdateTimestamp(currentTimeProvider.currentTimeMills()) }

    override fun getTokenIconURL(chainId: Int, address: String): Single<String> =
        cryptoApi.getTokenRawData(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.find { chainId == it.chainId && address == it.address }?.logoURI ?: String.Empty
        }

    override fun sortTokensByChainId(
        tokenList: List<ERC20Token>
    ): Map<Int, List<ERC20Token>> =
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

    //TODO add refreshing optimalization for tokens for current account
    override fun refreshTokenBalance(account: Account): Single<Pair<String, List<AccountToken>>> =
        loadCurrentTokens(account.chainId).let { tokens ->
            Observable.fromIterable(tokens)
                .flatMap {
                    blockchainRepository.refreshTokenBalance(account.privateKey, account.chainId, it.address, account.address)
                }
                .map { (address, balance) ->
                    tokens.find { it.address == address }?.let { erc20Token ->
                        AccountToken(
                            erc20Token,
                            balance,
                            tempStorage.getRate(generateTokenHash(erc20Token.chainId, erc20Token.address))
                        )
                    }.orElse { throw NullPointerException() }
                }
                .toList()
                .map { Pair(account.privateKey, it.toList()) }
        }

    override fun downloadTokensList(account: Account): Single<List<ERC20Token>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> getEthereumTokens(account)
            else -> getNotEthereumTokens(account)
        }

    private fun updateAccountTokenRate(token: ERC20Token, ratesMap: Map<String, Double>): Observable<Pair<String, Double>> =
        generateTokenHash(token.chainId, token.address).let { tokenHash ->
            ratesMap[tokenHash]?.let {
                Log.e("klop", "Using storaget local data")
                Observable.just(Pair(tokenHash, it))
            }.orElse {
                cryptoApi.getTokenMarkets(MarketUtils.getMarketId(token.chainId), token.address)
                    .onErrorReturn { TokenMarketResponse(marketData = MarketData(FiatPrice())) }
                    .map {
                        val tokenMarketRate = it.marketData.currentFiatPrice.getRate(localStorage.loadCurrentFiat())
                        Log.e("klop", "Current Fiat: ${localStorage.loadCurrentFiat()} with rate: $tokenMarketRate")
                        Pair(tokenHash, tokenMarketRate)
                    }.toObservable()
            }
        }

    override fun getTokensRate(tokens: Map<Int, List<ERC20Token>>): Completable =
        mutableListOf<Observable<Pair<String, Double>>>().let { observables ->
            if (currentFiat != localStorage.loadCurrentFiat()) tempStorage.clearRates()
            tokens.values.forEach {
                it.forEach {
                    observables.add(updateAccountTokenRate(it, tempStorage.getRates()))
                }
            }
            Observable.merge(observables)
                .doOnNext { (tokenHash, rate) ->
                    tempStorage.saveRate(tokenHash, rate)
                }.toList()
                .doOnSuccess {
                    currentFiat = localStorage.loadCurrentFiat()
                }
                .ignoreElement()
        }

    override fun updateTokensRate(account: Account) {
        account.apply {
            accountTokens.forEach {
                it.tokenPrice = tempStorage.getRate(generateTokenHash(it.token.chainId, it.token.address))
            }
        }
    }

    private fun getNotEthereumTokens(account: Account): Single<List<ERC20Token>> =
        cryptoApi.getConnectedTokens(url = getTokensApiURL(account))
            .map { response ->
                mutableListOf<ERC20Token>().apply {
                    response.tokens.forEach {
                        add(TokenDataToERC20Token.map(account.chainId, it))
                    }
                }
            }

    private fun getEthereumTokens(account: Account): Single<List<ERC20Token>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map { response ->
                mutableListOf<ERC20Token>().apply {
                    response.tokens.forEach {
                        add(ERC20Token(account.chainId, it))
                    }
                }
            }

    private fun getTokenTxApiURL(account: Account) =
        String.format(ETHEREUM_TOKENTX_REQUEST, getTokenBalanceURL(account.chainId), account.address, ETHERSCAN_KEY)

    @VisibleForTesting
    fun getTokensApiURL(account: Account) =
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

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenRawData(url = ERC20_TOKEN_DATA_URL).map { data ->
            data.associate { generateTokenHash(it.chainId, it.address) to it.logoURI }
        }

    private fun updateAllTokenIcons(updatedIcons: Map<String, String>): Completable =
        walletManager.getWalletConfig().run {
            erc20Tokens.forEach { (key, value) ->
                value.forEach {
                    it.logoURI = updatedIcons[generateTokenHash(key, it.address)]
                }
            }
            walletManager.updateWalletConfig(copy(version = updateVersion))
        }

    @VisibleForTesting
    fun generateTokenHash(chainId: Int, address: String) = "$chainId$address".toLowerCase(Locale.ROOT)

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
            }
        }

    /**
     *
     * arguments: map - downloaded tokens - Map<AccountPrivateKey, List<AccountToken>>, tokens - MutableMap<ChainId, List<ERC20Token>>
     * return statement: Map<ChainId, List<ERC20Token>>
     */

    private fun updateTokens(map: Map<Int, List<ERC20Token>>, tokens: MutableMap<Int, List<ERC20Token>>) =
        tokens.apply {
            map.values.forEach {
                it.forEach { accountToken ->
                    (this[accountToken.chainId] ?: listOf()).toMutableList().let { currentTokens ->
                        currentTokens.removeAll { it.address == accountToken.address }
                        currentTokens.add(accountToken)
                        put(accountToken.chainId, currentTokens)
                    }
                }
            }
        }

    private fun checkUpdates(list: List<CommitElement>): Boolean =
        list[LAST_UPDATE_INDEX].commit.committer.date.let {
            localStorage.loadTokenIconsUpdateTimestamp() < DateUtils.getTimestampFromDate(it)
        }

    companion object {
        private const val LAST_UPDATE_INDEX = 0
        private const val TOKEN_BALANCE_REQUEST = "%sapi?module=account&action=tokenlist&address=%s"
        private const val ETHEREUM_TOKENTX_REQUEST =
            "%sapi?module=account&action=tokentx&address=%s&startblock=0&endblock=999999999&sort=asc&apikey=%s"
    }
}