package minerva.android.walletmanager.manager.accounts.tokens

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.TokenBalance
import minerva.android.apiProvider.model.TokenTx
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
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_SIGMA
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ATS_TAU
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_GOR
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_KOV
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_RIN
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.ETH_ROP
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.LUKSO_14
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_CORE
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.POA_SKL
import minerva.android.walletmanager.model.defs.NetworkShortName.Companion.XDAI
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

    override fun loadCurrentTokens(network: String): List<ERC20Token> =
        walletManager.getWalletConfig()?.let {
            NetworkManager.getTokens(network)
                .mergeWithoutDuplicates(it.erc20Tokens[network] ?: listOf())
        } ?: listOf()

    override fun saveToken(network: String, token: ERC20Token): Completable =
        walletManager.getWalletConfig()?.let { config ->
            config.copy(
                version = config.updateVersion,
                erc20Tokens = updateTokens(network, token, config.erc20Tokens.toMutableMap())
            ).let { walletManager.updateWalletConfig(it) }
        } ?: Completable.error(NotInitializedWalletConfigThrowable())

    override fun saveTokens(map: Map<String, List<AccountToken>>): Completable =
        walletManager.getWalletConfig()?.let { config ->
            config.copy(
                version = config.updateVersion,
                erc20Tokens = updateTokens(map, config.erc20Tokens.toMutableMap())
            ).let { walletManager.updateWalletConfig(it) }
        } ?: Completable.error(NotInitializedWalletConfigThrowable())

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

    override fun mapToAccountTokensList(
        network: String,
        tokenMap: Map<String, TokenBalance>
    ): List<AccountToken> =
        mutableListOf<AccountToken>().apply {
            loadCurrentTokens(network).forEach {
                tokenMap[it.address]?.let { tokenBalance ->
                    add(mapToERC20Token(network, tokenBalance))
                }.orElse { add(AccountToken(it, BigDecimal.ZERO)) }
            }
            tokenMap.values.forEach { tokenBalance ->
                if (find { it.token.address == tokenBalance.address } == null)
                    add(mapToERC20Token(network, tokenBalance))
            }
            //TODO need to be sorted by fiat value, when getting values will be implemented
        }.sortedByDescending { it.balance }

    override fun updateTokensFromLocalStorage(map: Map<String, List<AccountToken>>): Pair<Boolean, Map<String, List<AccountToken>>> =
        walletManager.getWalletConfig()?.erc20Tokens?.let { localTokens ->
            var updateTokens = false
            map.values.forEach { accountTokenList ->
                accountTokenList.forEach { accountToken ->
                    NetworkManager.getShort(accountToken.token.chainId).let { network ->
                        localTokens[network]?.find { it == accountToken.token }?.logoURI.let {
                            accountToken.token.logoURI = it
                            if (it == null) updateTokens = true
                        }
                    }
                }
            }
            Pair(updateTokens, map)
        }.orElse { throw NotInitializedWalletConfigThrowable() }


    override fun updateTokens(localCheckResult: Pair<Boolean, Map<String, List<AccountToken>>>): Single<Map<String, List<AccountToken>>> =
        if (localCheckResult.first) {
            getTokenIconsURL().map { logoUrls ->
                val updatedTokensMap = mutableMapOf<String, List<ERC20Token>>()
                localCheckResult.second.values.forEach { accountTokens ->
                    var network = String.Empty
                    val updatedTokens = mutableListOf<ERC20Token>()
                    accountTokens.forEach {
                        it.token.apply {
                            if (logoURI == null) {
                                network = NetworkManager.getShort(chainId)
                                logoURI = logoUrls[generateTokenIconKey(chainId, address)]
                                updatedTokens.add(this)
                            }
                        }
                    }
                    if (updatedTokens.isNotEmpty()) updatedTokensMap[network] = updatedTokens
                }
                localCheckResult.second
            }
        } else Single.just(localCheckResult.second)

    override fun getTokenBalance(account: Account): Single<List<TokenBalance>> =
        when (account.networkShort) {
            ETH_MAIN, ETH_RIN, ETH_ROP, ETH_KOV, ETH_GOR -> getEthereumTokenBalance(account)
            else -> cryptoApi.getTokenBalance(url = getTokensApiURL(account)).map { it.tokens }
        }

    //TODO finish implementing it
    private fun getEthereumTokenBalance(account: Account): Single<List<TokenBalance>> =
        cryptoApi.getTokenTx(url = getTokenTxApiURL(account))
            .map {
                Log.e("klop", "Raw response H E R E ! ${it.tokens.size}")
                val tokensMap = it.tokens.map { it.address to it }
                Log.e("klop", "Tokens Map: ${tokensMap.size}")
                listOf<TokenBalance>()
            }

    private fun getTokenTxApiURL(account: Account) =
        //TODO klop USE CORRECT DATA
        String.format(
            ETHEREUM_TOKENTX_REQUEST,
            getTokenBalanceURL(ETH_GOR),
            "0x30B125d5Fc58c1b8E3cCB2F1C71a1Cc847f024eE",
            ETHERSCAN_KEY
        )

    @VisibleForTesting
     fun getTokensApiURL(account: Account) =
        String.format(TOKEN_BALANCE_REQUEST, getTokenBalanceURL(account.networkShort), account.address)

    private fun mapToERC20Token(network: String, token: TokenBalance): AccountToken =
        AccountToken(
            ERC20Token(NetworkManager.getChainId(network), token.name, token.symbol, token.address, token.decimals),
            blockchainRepository.fromGwei(token.balance.toBigDecimal())
        )

    private fun getTokenBalanceURL(networkShort: String) =
        when (networkShort) {
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
                    it.logoURI = updatedIcons[generateTokenIconKey(NetworkManager.getChainId(key), it.address)]
                }
            }
            walletManager.updateWalletConfig(config.copy(version = config.updateVersion))
        } ?: Completable.error(NotInitializedWalletConfigThrowable())

    @VisibleForTesting
    fun generateTokenIconKey(chainId: Int, address: String) = "$chainId$address"

    @VisibleForTesting
    fun updateTokens(network: String, token: ERC20Token, tokens: MutableMap<String, List<ERC20Token>>) =
        tokens.apply {
            (this[network] ?: listOf()).toMutableList().let { currentTokens ->
                currentTokens.removeAll { it.address == token.address }
                currentTokens.add(token)
                put(network, currentTokens)
            }
        }

    private fun updateTokens(map: Map<String, List<AccountToken>>, tokens: MutableMap<String, List<ERC20Token>>) =
        tokens.apply {
            map.values.forEach {
                it.forEach { accountToken ->
                    val network = NetworkManager.getShort(accountToken.token.chainId)
                    (this[network] ?: listOf()).toMutableList().let { currentTokens ->
                        currentTokens.removeAll { it.address == accountToken.token.address }
                        currentTokens.add(accountToken.token)
                        put(network, currentTokens)
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