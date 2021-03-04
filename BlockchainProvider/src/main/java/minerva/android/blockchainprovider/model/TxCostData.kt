package minerva.android.blockchainprovider.model

import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal

data class TxCostData(
    val transferType: BlockchainTransactionType,
    val from: String = String.Empty,
    val to: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO,
    val chainId: Int = Int.InvalidValue,
    val tokenDecimals: Int = Int.InvalidValue,
    val contractAddress: String = String.Empty,
    val contractData: String = String.Empty
)
