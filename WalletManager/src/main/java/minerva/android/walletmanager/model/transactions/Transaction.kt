package minerva.android.walletmanager.model.transactions

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal
import java.math.BigInteger

data class Transaction(
    val address: String = String.Empty,
    val privateKey: String = String.Empty,
    val receiverKey: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO,
    val gasPrice: BigDecimal = BigDecimal.ZERO,
    val gasLimit: BigInteger = BigInteger.ZERO,
    val contractAddress: String = String.Empty,
    val data: String = String.Empty,
    val tokenDecimals: Int = Int.InvalidValue
)