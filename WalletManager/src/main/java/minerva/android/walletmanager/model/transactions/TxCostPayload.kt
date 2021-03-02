package minerva.android.walletmanager.model.transactions

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.defs.TransferType
import java.math.BigDecimal

data class TxCostPayload(
    val transferType: TransferType,
    val networkShort: String = String.Empty,
    val from: String = String.Empty,
    val to: String = String.Empty,
    val amount: BigDecimal = BigDecimal.ZERO,
    val chainId: Int = Int.InvalidValue,
    val contractAddress: String = String.Empty,
    val contractData: String = String.Empty
)