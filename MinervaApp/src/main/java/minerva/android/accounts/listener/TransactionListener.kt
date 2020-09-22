package minerva.android.accounts.listener

import minerva.android.kotlinUtils.Empty

interface TransactionListener : AddressScannerListener {
    fun onTransactionAccepted(message: String? = String.Empty)
    fun onError(message: String)
}
