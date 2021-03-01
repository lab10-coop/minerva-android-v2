package minerva.android.blockchainprovider.model

import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class TxCostData(
    val networkShort: String,
    val tokenIndex: Int,
    val from: String,
    val to: String,
    val amount: BigDecimal,
    val chainId: Int,
    val contractAddress: String,
    val contractData: String = String.Empty
)
