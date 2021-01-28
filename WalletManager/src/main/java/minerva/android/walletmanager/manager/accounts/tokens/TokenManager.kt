package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token
import java.math.BigDecimal

interface TokenManager {
    fun loadTokens(network: String): List<ERC20Token>
    fun saveToken(network: String, token: ERC20Token): Completable
    fun mapToAccountTokensList(network: String, tokenList: List<Pair<String, BigDecimal>>): List<AccountToken>
}