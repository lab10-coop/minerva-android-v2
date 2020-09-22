package minerva.android.blockchainprovider.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

data class PendingTransaction(
    val index: Int,
    val txHash: String = String.Empty,
    val network: String = String.Empty,
    val senderAddress: String = String.Empty,
    val blockNumber: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO
)