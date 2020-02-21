package minerva.android.blockchainprovider.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

data class TransactionPayload(
    val address: String = String.Empty,
    val privateKey: String = String.Empty,
    val receiverKey: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ONE,
    val gasPrice: BigDecimal = BigDecimal.ONE,
    val gasLimit: BigInteger = BigInteger.ZERO,
    val contractAddress: String = String.Empty
)