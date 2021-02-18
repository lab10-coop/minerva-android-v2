package minerva.android.walletmanager.model.walletconnect

import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.utils.BalanceUtils

data class WalletConnectTransaction(
    val from: String,
    val to: String,
    val nonce: String?,
    val gasPrice: String?,
    val gasLimit: String?,
    var value: String,
    val fiatValue: Double?,
    val data: String,
    val txCost: TransactionCost = TransactionCost()
) {
    val fiatWithUnit = "${fiatValue.toString()} EUR"
}
