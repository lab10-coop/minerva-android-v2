package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.kotlinUtils.list.removeAll
import minerva.android.walletmanager.BuildConfig.*
import minerva.android.walletmanager.exception.AllTokenIconsUpdated
import minerva.android.walletmanager.exception.NetworkNotFoundThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
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
import minerva.android.walletmanager.model.mappers.TokenBalanceToAccountToken
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage
import java.math.BigDecimal

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val blockchainRepository: BlockchainRegularAccountRepository
) : TokenManager {

    private val currentTimeProvider = CurrentTimeProviderImpl()

    override fun loadCurrentTokens(chainId: Int): List<ERC20Token> =
        walletManager.getWalletConfig()?.let {
            NetworkManager.getTokens(chainId)
                .mergeWithoutDuplicates(it.erc20Tokens[chainId] ?: listOf())
        } ?: listOf()

    override fun saveToken(chainId: Int, token: ERC20Token): Completable =
        walletManager.getWalletConfig()?.let { config ->
            config.copy(
                version = config.updateVersion,
                erc20Tokens = updateTokens(chainId, token, config.erc20Tokens.toMutableMap())
            ).let { walletManager.updateWalletConfig(it) }
        } ?: Completable.error(NotInitializedWalletConfigThrowable())

    override fun saveTokens(shouldBeSaved: Boolean, map: Map<String, List<AccountToken>>): Completable =
        if (shouldBeSaved) {
            walletManager.getWalletConfig()?.let { config ->
                config.copy(
                    version = config.updateVersion,
                    erc20Tokens = updateTokens(map, config.erc20Tokens.toMutableMap())
                ).let { walletManager.updateWalletConfig(it) }
            } ?: Completable.error(NotInitializedWalletConfigThrowable())
        } else Completable.complete()

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

    override fun prepareCurrentTokenList(
        chainId: Int,
        tokenList: List<AccountToken>
    ): List<AccountToken> =
        mutableListOf<AccountToken>().apply {
            addAll(tokenList)
            loadCurrentTokens(chainId).forEach {
                val token = AccountToken(it, BigDecimal.ZERO)
                if (!contains(token)) {
                    add(token)
                }
            }
            //TODO need to be sorted by fiat value, when getting values will be implemented
        }.sortedByDescending { it.balance }

    override fun updateTokensFromLocalStorage(map: Map<String, List<AccountToken>>): Pair<Boolean, Map<String, List<AccountToken>>> =
        walletManager.getWalletConfig()?.erc20Tokens?.let { localTokens ->
            var updateTokens = false
            map.values.forEach { accountTokenList ->
                accountTokenList.forEach { accountToken ->
                    localTokens[accountToken.token.chainId]?.find { it == accountToken.token }?.logoURI.let {
                        accountToken.token.logoURI = it
                        if (it == null) updateTokens = true
                    }
                }
            }
            Pair(updateTokens, map)
        }.orElse { throw NotInitializedWalletConfigThrowable() }

    override fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        accountTokens: Map<String, List<AccountToken>>
    ): Single<Pair<Boolean, Map<String, List<AccountToken>>>> =
        if (shouldBeUpdated) {
            var shouldBeSaved = false
            getTokenIconsURL().map { logoUrls ->
                accountTokens.values.forEach { accountTokens ->
                    accountTokens.forEach {
                        it.token.apply {
                            logoUrls[generateTokenIconKey(chainId, address)]?.let { newLogoURI ->
                                logoURI = newLogoURI
                                shouldBeSaved = true
                            }
                        }
                    }
                }
                Pair(shouldBeSaved, accountTokens)
            }
        } else Single.just(Pair(false, accountTokens))

    override fun refreshTokenBalance(account: Account): Single<List<AccountToken>> =
        when (account.chainId) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> getEthereumTokenBalance(account)
            else -> cryptoApi.getTokenBalance(url = getTokensApiURL(account)).map {
                mutableListOf<AccountToken>().apply {
                    it.tokens.forEach {
                        add(TokenBalanceToAccountToken.map(account.chainId, it))
                    }
                }
            }
        }

    private fun getEthereumTokenBalance(account: Account): Single<List<AccountToken>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map { response -> response.tokens.map { it.address to it }.toMap().values.toList() }
            .flatMap { tokens ->
                Observable.range(FIRST_INDEX, tokens.size)
                    .flatMap { position ->
                        blockchainRepository.refreshTokenBalance(
                            account.privateKey,
                            account.chainId,
                            tokens[position].address,
                            account.address
                        )
                    }.toList()
                    .map {
                        mutableListOf<AccountToken>().apply {
                            it.forEach { (address, balance) ->
                                tokens.find { it.address == address }?.let { tokenTx ->
                                    add(AccountToken(ERC20Token(account.network.chainId, tokenTx), balance))
                                }
                            }
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
            data.associate { generateTokenIconKey(it.chainId, it.address) to it.logoURI }
        }

    private fun updateAllTokenIcons(updatedIcons: Map<String, String>): Completable =
        walletManager.getWalletConfig()?.let { config ->
            config.erc20Tokens.forEach { (key, value) ->
                value.forEach {
                    it.logoURI = updatedIcons[generateTokenIconKey(key, it.address)]
                }
            }
            walletManager.updateWalletConfig(config.copy(version = config.updateVersion))
        } ?: Completable.error(NotInitializedWalletConfigThrowable())

    @VisibleForTesting
    fun generateTokenIconKey(chainId: Int, address: String) = "$chainId$address"

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

    private fun updateTokens(map: Map<String, List<AccountToken>>, tokens: MutableMap<Int, List<ERC20Token>>) =
        tokens.apply {
            map.values.forEach {
                it.forEach { accountToken ->
                    (this[accountToken.token.chainId] ?: listOf()).toMutableList().let { currentTokens ->
                        currentTokens.removeAll { it.address == accountToken.token.address }
                        currentTokens.add(accountToken.token)
                        put(accountToken.token.chainId, currentTokens)
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
        private const val FIRST_INDEX = 0
        private const val TOKEN_BALANCE_REQUEST = "%sapi?module=account&action=tokenlist&address=%s"
        private const val ETHEREUM_TOKENTX_REQUEST =
            "%sapi?module=account&action=tokentx&address=%s&startblock=0&endblock=999999999&sort=asc&apikey=%s"
    }
}