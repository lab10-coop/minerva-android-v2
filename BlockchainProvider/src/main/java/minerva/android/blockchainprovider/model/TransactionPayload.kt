package minerva.android.blockchainprovider.model

import java.math.BigDecimal
import java.math.BigInteger

data class TransactionPayload(
    val address: String,
    val privateKey: String,
    val receiverKey: String,
    val amount: BigDecimal,
    val gasPrice: BigDecimal,
    val gasLimit: BigInteger
)