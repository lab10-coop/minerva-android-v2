package minerva.android.walletmanager.model

import java.math.BigDecimal
import java.math.BigInteger

data class Transaction(
    val address: String,
    val privateKey: String,
    val receiverKey: String,
    val amount: BigDecimal,
    val gasPrice: BigDecimal,
    val gasLimit: BigInteger
)