package minerva.android.walletmanager.model.walletconnect

import minerva.android.walletmanager.model.transactions.TransactionCost

data class WalletConnectTransaction(
    val from: String,
    val to: String,
    val nonce: String?,
    val gasPrice: String?,
    val gasLimit: String?,
    var value: String,
    val fiatValue: String?,
    val data: String,
    val txCost: TransactionCost = TransactionCost()
) {
    val fiatWithUnit = "$fiatValue EUR"
}
