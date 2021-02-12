package minerva.android.walletmanager.manager.accounts.tokens

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.TokenBalance
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.kotlinUtils.list.removeAll
import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.exception.AllTokenIconsUpdated
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.storage.LocalStorage

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val blockchainRepository: BlockchainRegularAccountRepository
) : TokenManager {

    private val currentTimeProvider = CurrentTimeProviderImpl()

    override fun loadTokens(network: String): List<ERC20Token> =
        walletManager.getWalletConfig()?.let {
            NetworkManager.getTokens(network)
                .mergeWithoutDuplicates(it.erc20Tokens[network] ?: listOf())
                .sortedBy { it.name }
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
        cryptoApi.getTokenLastCommitRawData(url = BuildConfig.ERC20_TOKEN_DATA_LAST_COMMIT)
            .flatMap {
                if (checkUpdates(it)) getTokenIconsURL()
                else Single.error(AllTokenIconsUpdated())
            }
            .flatMapCompletable { updateAllTokenIcons(it) }
            .doOnComplete { localStorage.saveTokenIconsUpdateTimestamp(currentTimeProvider.currentTimeMills()) }

    override fun getTokenIconURL(chainId: Int, address: String): Single<String> =
        cryptoApi.getTokenRawData(url = BuildConfig.ERC20_TOKEN_DATA_URL).map { data ->
            data.find { chainId == it.chainId && address == it.address }?.logoURI ?: String.Empty
        }

    override fun mapToAccountTokensList(
        network: String,
        tokenList: List<Pair<String, TokenBalance>>
    ): List<AccountToken> =
        loadTokens(network).let { allNetworkTokens ->
            tokenList.map { getTokenFromPair(network, allNetworkTokens, it) }
        }

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

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenRawData(url = BuildConfig.ERC20_TOKEN_DATA_URL).map { data ->
            data.associate { generateTokenIconKey(it.chainId, it.address) to it.logoURI }
        }

    private fun getTokenFromPair(network: String, allTokens: List<Token>, raw: Pair<String, TokenBalance>): AccountToken =
        with(raw.second) {
            ((allTokens.find { (it as? ERC20Token)?.address == raw.first } as? ERC20Token)
                ?: ERC20Token(NetworkManager.getChainId(network), name, symbol, address, decimals)).let {
                AccountToken(it, blockchainRepository.fromGwei(balance.toBigDecimal()))
            }
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
            Log.e("klop", "Update wallet config N O W !")
            map.values.forEach {
                it.forEach { accountToken ->
                    val network = NetworkManager.getShort(accountToken.token.chainId)
                    (this[network] ?: listOf()).toMutableList().let { currentTokens ->
                        Log.e("klop", "Befrore ${currentTokens.size}")
                        currentTokens.removeAll { it.address == accountToken.token.address }
                        currentTokens.add(accountToken.token)
                        put(network, currentTokens)
                        Log.e("klop", "After ${currentTokens.size}")
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
    }
}