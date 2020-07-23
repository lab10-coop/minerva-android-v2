package minerva.android.walletmanager.model

import java.math.BigDecimal
import java.math.BigInteger

data class TransactionCost(
    val gasPrice: BigDecimal,
    val gasLimit: BigInteger,
    val cost: BigDecimal
)