package minerva.android.blockchainprovider.repository.erc20

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import minerva.android.blockchainprovider.model.Token
import minerva.android.blockchainprovider.model.TransactionPayload
import java.math.BigInteger

interface ERC20TokenRepository {
    fun getERC20TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC20TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC20TokenDecimals(privateKey: String, chainId: Int, tokenAddress: String): Observable<BigInteger>
    fun transferERC20Token(chainId: Int, payload: TransactionPayload): Completable
    fun getTokenBalance(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        safeAccountAddress: String
    ): Flowable<Token>
}