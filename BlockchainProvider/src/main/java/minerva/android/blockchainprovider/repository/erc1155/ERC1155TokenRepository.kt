package minerva.android.blockchainprovider.repository.erc1155

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TransactionPayload
import java.math.BigInteger

interface ERC1155TokenRepository {
    fun getERC1155DetailsUri(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger
    ): Single<String>

    fun getTokenBalance(
        tokenId: String,
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        ownerAddress: String
    ): Flowable<Token>

    fun transferERC1155Token(chainId: Int, payload: TransactionPayload): Completable
}