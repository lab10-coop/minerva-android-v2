package minerva.android.blockchainprovider.model

import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class TxCostData(
    val transferType: BlockchainTransactionType,
    val networkShort: String,
    val from: String,
    val to: String,
    val amount: BigDecimal,
    val chainId: Int,
    val contractAddress: String,
    val contractData: String = String.Empty
)
