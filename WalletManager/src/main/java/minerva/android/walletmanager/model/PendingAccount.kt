package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

data class PendingAccount(
    val index: Int,
    val txHash: String = String.Empty,
    val network: String = String.Empty,
    val senderAddress: String = String.Empty,
    var blockHash: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val blockNumber: BigInteger = BigInteger.ONE
)