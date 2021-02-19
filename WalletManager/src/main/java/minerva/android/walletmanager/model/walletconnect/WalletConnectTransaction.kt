package minerva.android.walletmanager.model.walletconnect

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.transactions.TransactionCost

data class WalletConnectTransaction(
    val from: String = String.Empty,
    val to: String = String.Empty,
    val nonce: String? = null,
    val gasPrice: String? = null,
    val gasLimit: String? = null,
    var value: String = String.Empty,
    val fiatValue: String? = null,
    val data: String = String.Empty,
    val txCost: TransactionCost = TransactionCost()
) {
    val fiatWithUnit = "$fiatValue EUR"
}
