package minerva.android.walletmanager.model.transactions

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class TxCostPayload(
    //todo add enum types for transaction kinds
    val networkShort: String,
    val tokenIndex: Int,
    val from: String,
    val to: String,
    val amount: BigDecimal,
    val chainId: Int,
    val contractAddress: String = String.Empty,
    val contractData: String = String.Empty
)