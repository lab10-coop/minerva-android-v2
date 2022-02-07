package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.Asset
import minerva.android.walletmanager.model.token.ActiveSuperToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.UpdateTokensResult

interface TokenManager {
    fun checkMissingTokensDetails(): Completable
    fun getActiveTokensPerAccount(account: Account): List<ERCToken>
    fun saveToken(accountAddress: String, chainId: Int, token: ERCToken): Completable
    fun saveTokens(
        shouldSafeNewTokens: Boolean,
        newAndLocalTokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<Boolean>

    fun getTokenIconURL(chainId: Int, address: String): Single<String>
    fun sortTokensByChainId(tokenList: List<ERCToken>): Map<Int, List<ERCToken>>

    /**
     * arguments: Map<ChainId, List<ERC20Token>>
     *     return statement: Pair<isUpdated, Map<ChainId<List<ERC20Token>>>
     */
    fun mergeWithLocalTokensList(newTokensPerChainIdMap: Map<Int, List<ERCToken>>): UpdateTokensResult

    /**
     * return statement: Map<ChainId, List<ERC20Token>>
     */
    fun updateTokenIcons(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<UpdateTokensResult>

    fun getTokenBalance(account: Account): Flowable<Asset>
    fun downloadTokensList(account: Account): Single<List<ERCToken>>
    fun getTokensRates(tokens: Map<Int, List<ERCToken>>): Completable
    fun updateTokensRate(account: Account)
    fun getSingleTokenRate(tokenHash: String): Double
    fun getTaggedTokensUpdate(): Flowable<List<ERCToken>>
    fun getTaggedTokensSingle(): Single<List<ERCToken>>
    fun getSuperTokenBalance(account: Account): Flowable<Asset>
    var activeSuperTokenStreams: MutableList<ActiveSuperToken>
    fun mergeNFTDetailsWithRemoteConfig(
        shouldBeUpdated: Boolean,
        tokensPerChainIdMap: Map<Int, List<ERCToken>>
    ): Single<UpdateTokensResult>

    fun getNftsPerAccount(
        chainId: Int,
        accountAddress: String,
        collectionAddress: String
    ): List<ERCToken>

    fun updateMissingNFTTokensDetails(
        tokensPerChainIdMap: Map<Int, List<ERCToken>>,
        accounts: List<Account>
    ): Single<UpdateTokensResult>

    fun hasTokenExplorer(chainId: Int) : Boolean

    fun fetchNFTsDetails() : Single<Boolean>
}