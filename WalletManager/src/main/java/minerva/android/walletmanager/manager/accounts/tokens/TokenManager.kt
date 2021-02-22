package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token

interface TokenManager {
    fun updateTokenIcons(): Completable
    fun loadCurrentTokens(network: String): List<ERC20Token>
    fun saveToken(network: String, token: ERC20Token): Completable
    fun saveTokens(map: Map<String, List<AccountToken>>): Completable
    fun getTokenIconURL(chainId: Int, address: String): Single<String>
    fun prepareCurrentTokenList(network: String, tokenList: List<AccountToken>): List<AccountToken>
    fun updateTokensFromLocalStorage(map: Map<String, List<AccountToken>>): Pair<Boolean, Map<String, List<AccountToken>>>
    fun updateTokens(localCheckResult: Pair<Boolean, Map<String, List<AccountToken>>>): Single<Map<String, List<AccountToken>>>
    fun refreshTokenBalance(account: Account): Single<List<AccountToken>>
}