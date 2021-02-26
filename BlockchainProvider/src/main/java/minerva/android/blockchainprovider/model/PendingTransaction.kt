package minerva.android.blockchainprovider.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal
import java.math.BigInteger

data class PendingTransaction(
    val index: Int,
    val txHash: String = String.Empty,
    //TODO adding invalid value seems to be dangerous
    val chainId: Int = Int.InvalidValue,
    val senderAddress: String = String.Empty,
    val blockHash: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO,
    val blockNumber: BigInteger = BigInteger.ONE
)