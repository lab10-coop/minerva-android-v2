package minerva.android.blockchainprovider.repository.superToken

import io.reactivex.Flowable
import java.math.BigInteger

interface SuperTokenRepository {
    fun getNetFlow(
        cfaAddress: String,
        chainId: Int,
        privateKey: String,
        tokenAddress: String,
        accountAddress: String
    ): Flowable<BigInteger>
}