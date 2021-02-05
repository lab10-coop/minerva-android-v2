package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import java.math.BigDecimal

interface TokenManager {
    fun loadTokens(network: String): List<ERC20Token>
    fun saveToken(network: String, token: ERC20Token): Completable
    fun mapToAccountTokensList(network: String, tokenList: List<Pair<String, BigDecimal>>): List<AccountToken>
    fun updateTokenIcons(): Completable
    fun getTokenIconURL(chainId: Int, address: String): Single<String>
}