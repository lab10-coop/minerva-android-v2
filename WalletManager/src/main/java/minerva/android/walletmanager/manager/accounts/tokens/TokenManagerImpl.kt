package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.kotlinUtils.list.removeAll
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token
import java.math.BigDecimal

class TokenManagerImpl(private val walletManager: WalletConfigManager) : TokenManager {

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

    override fun mapToAccountTokensList(network: String, tokenList: List<Pair<String, BigDecimal>>): List<AccountToken> =
        loadTokens(network).let { allNetworkTokens ->
            tokenList.map {
                getTokenFromPair(allNetworkTokens, it)
            }
        }

    private fun getTokenFromPair(allTokens: List<Token>, raw: Pair<String, BigDecimal>): AccountToken {
        val token = (allTokens.find { (it as? ERC20Token)?.address == raw.first } as? ERC20Token) ?: ERC20Token(address = raw.first)
        return AccountToken(token, raw.second)
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
}