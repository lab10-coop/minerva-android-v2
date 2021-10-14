package minerva.android.blockchainprovider.repository.erc721

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.Token
import java.math.BigInteger

interface ERC721TokenRepository {
    fun getERC721TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC721TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC721DetailsUri(privateKey: String, chainId: Int, tokenAddress: String, tokenId: BigInteger): Single<String>
    fun getTokenBalance(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        safeAccountAddress: String
    ): Flowable<Token>

    fun isTokenOwner(
        tokenId: String,
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        safeAccountAddress: String
    ): Single<Boolean>
}