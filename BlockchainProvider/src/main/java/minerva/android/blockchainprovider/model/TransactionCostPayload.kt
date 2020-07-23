package minerva.android.blockchainprovider.model

import java.math.BigDecimal
import java.math.BigInteger

data class TransactionCostPayload(
    val gasPrice: BigDecimal,
    val gasLimit: BigInteger,
    val cost: BigDecimal
)