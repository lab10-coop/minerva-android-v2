package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.kotlinUtils.list.removeAll
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token
import java.math.BigDecimal

class TokenManagerImpl(private val walletManager: WalletConfigManager, private val tokenIconRepository: TokenIconRepository) :
    TokenManager {

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

    override fun getTokenIconURL(chainId: Int, address: String): Single<String> =
        Single.create {
            tokenIconRepository.getIconRawFile().let { data ->
                Gson().fromJson(data, Array<ERC20Token>::class.java).associateBy { generateTokenIconKey(it.chainId, it.address) }
                    .let { map ->
                        map[generateTokenIconKey(chainId, address)]?.logoURI?.let { result ->
                            it.onSuccess(result)
                        }.orElse {
                            it.onSuccess(String.Empty)
                        }
                    }
            }
        }

    @VisibleForTesting
    fun generateTokenIconKey(chainId: Int, address: String) = "$chainId$address"

    private fun getTokenFromPair(
        allTokens: List<Token>,
        raw: Pair<String, BigDecimal>
    ): AccountToken =
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
}