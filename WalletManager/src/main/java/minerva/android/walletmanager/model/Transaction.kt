package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

data class Transaction(
    val address: String = String.Empty,
    val privateKey: String = String.Empty,
    val receiverKey: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO,
    val gasPrice: BigDecimal = BigDecimal.ZERO,
    val gasLimit: BigInteger = BigInteger.ZERO,
    val contractAddress: String = String.Empty
)