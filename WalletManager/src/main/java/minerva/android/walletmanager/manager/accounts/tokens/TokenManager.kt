package minerva.android.walletmanager.manager.accounts.tokens

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.Asset
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.UpdateTokensResult
import java.math.BigInteger

interface TokenManager {
    fun checkMissingTokensDetails(): Completable
    fun getActiveTokensPerAccount(account: Account): List<ERCToken>
    fun saveToken(accountAddress: String, chainId: Int, newToken: ERCToken): Completable
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

    fun getTokenBalance(account: Account): Flowable<Asset>
    fun downloadTokensList(account: Account): Single<List<ERCToken>>
    fun getTokensRates(tokens: Map<Int, List<ERCToken>>): Completable
    fun updateTokensRate(account: Account)
    fun getSingleTokenRate(tokenHash: String): Double
    fun getTokensUpdate(): Flowable<List<ERCToken>>
    fun getSuperTokenBalance(account: Account): Flowable<Asset>
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

    fun getERC721TokenDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String
    ): Single<ERCToken>

    fun updateMissingERC721TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String,
        token: ERCToken
    ): Single<ERCToken>

    fun getERC1155TokenDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String
    ): Single<ERCToken>

    fun updateMissingERC1155TokensDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        tokenUri: String,
        token: ERCToken
    ): Single<ERCToken>

    fun isNftOwner(
        type: TokenType, tokenId: String,
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        ownerAddress: String
    ): Single<Boolean>
}