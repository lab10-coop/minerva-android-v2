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
    fun saveTokens(shouldBeSaved: Boolean, map: Map<Int, List<ERC20Token>>): Single<Boolean>
    fun getTokenIconURL(chainId: Int, address: String): Single<String>
    fun sortTokensByChainId(tokenList: List<ERC20Token>): Map<Int, List<ERC20Token>>

    /**
     * arguments: Map<ChainId, List<ERC20Token>>
     *     return statement: Pair<isUpdated, Map<ChainId<List<ERC20Token>>>
     */
    fun mergeWithLocalTokensList(map: Map<Int, List<ERC20Token>>): Pair<Boolean, Map<Int, List<ERC20Token>>>

    /**
     * return statement: Map<ChainId, List<ERC20Token>>
     */
    fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        accountTokens: Map<Int, List<ERC20Token>>
    ): Single<Pair<Boolean, Map<Int, List<ERC20Token>>>>

    fun refreshTokenBalance(account: Account): Single<Pair<String, List<AccountToken>>>
    fun downloadTokensList(account: Account): Single<List<ERC20Token>>
}