package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal
import java.math.BigInteger

data class PendingAccount(
    val index: Int,
    val txHash: String = String.Empty,
    val chainId: Int = Int.InvalidValue,
    val senderAddress: String = String.Empty,
    var blockHash: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val blockNumber: BigInteger = BigInteger.ONE
)