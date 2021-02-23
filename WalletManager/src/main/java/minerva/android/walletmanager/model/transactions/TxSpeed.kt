package minerva.android.walletmanager.model.transactions

import minerva.android.walletmanager.model.defs.TxType
import java.math.BigDecimal

data class TxSpeed(
    val type: TxType,
    val value: BigDecimal
) {
    val label = "${value.toPlainString()} Gwei ${type.time}"
}