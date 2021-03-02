package minerva.android.walletmanager.model.transactions

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.defs.TransferType
import java.math.BigDecimal

data class TxCostPayload(
    val transferType: TransferType,
    val networkShort: String,
    val from: String,
    val to: String,
    val amount: BigDecimal,
    val chainId: Int,
    val contractAddress: String = String.Empty,
    val contractData: String = String.Empty
)