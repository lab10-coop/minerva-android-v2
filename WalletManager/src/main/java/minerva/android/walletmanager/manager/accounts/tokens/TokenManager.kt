package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token

interface TokenManager {
    fun updateTokenIcons(): Completable
    fun loadCurrentTokens(chainId: Int): List<ERC20Token>
    fun saveToken(chainId: Int, token: ERC20Token): Completable
    /**
     * return statement: Map<AccountPrivateKey, List<AccountToken>>
     */
    fun saveTokens(map: Map<String, List<AccountToken>>): Completable
    fun getTokenIconURL(chainId: Int, address: String): Single<String>
    fun prepareCurrentTokenList(chainId: Int, tokenList: List<AccountToken>): List<AccountToken>

    /**
     * arguments: Map<AccountPrivateKey, List<AccountToken>>
     *     return statement: Pair<isUpdated, Map<AccountPrivateKey<List<AccountToken>>>
     */
    fun updateTokensFromLocalStorage(map: Map<String, List<AccountToken>>): Pair<Boolean, Map<String, List<AccountToken>>>

    /**
     * return statement: Map<AccountPrivateKey, List<AccountToken>>
     */
    fun updateTokens(shouldBeUpdated: Boolean, accountTokens: Map<String, List<AccountToken>>): Single<Map<String, List<AccountToken>>>
    fun refreshTokenBalance(account: Account): Single<List<AccountToken>>
}