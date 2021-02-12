package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.CommitElement
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
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
import java.math.BigDecimal

class TokenManagerImpl(
    private val walletManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage
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

    override fun mapToAccountTokensList(
        network: String,
        tokenList: List<Pair<String, BigDecimal>>
    ): List<AccountToken> =
        loadTokens(network).let { allNetworkTokens ->
            tokenList.map {
                getTokenFromPair(allNetworkTokens, it)
            }
        }

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

    private fun getTokenIconsURL(): Single<Map<String, String>> =
        cryptoApi.getTokenRawData(url = BuildConfig.ERC20_TOKEN_DATA_URL).map { data ->
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

    private fun getTokenFromPair(allTokens: List<Token>, raw: Pair<String, BigDecimal>): AccountToken =
        ((allTokens.find { (it as? ERC20Token)?.address == raw.first } as? ERC20Token)
            ?: ERC20Token(chainId = Int.InvalidValue, address = raw.first)).let {
            AccountToken(it, raw.second)
        }

    @VisibleForTesting
    fun updateTokens(network: String, token: ERC20Token, tokens: Map<String, List<ERC20Token>>) =
        tokens.toMutableMap().apply {
            (this[network] ?: listOf()).toMutableList().let { currentTokens ->
                currentTokens.removeAll { it.address == token.address }
                currentTokens.add(token)
                put(network, currentTokens)
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