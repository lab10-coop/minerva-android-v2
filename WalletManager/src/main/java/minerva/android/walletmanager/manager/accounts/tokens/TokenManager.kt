package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.Token
import java.math.BigDecimal

interface TokenManager : Manager {
    fun loadTokens(network: String): List<Token>
    fun saveToken(network: String, token: Token): Completable
    fun mapToAccountTokensList(network: String, tokenList: List<Pair<String, BigDecimal>>): List<AccountToken>
}