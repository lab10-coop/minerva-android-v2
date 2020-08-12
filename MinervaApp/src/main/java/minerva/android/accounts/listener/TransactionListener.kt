package minerva.android.accounts.listener

interface TransactionListener : AddressScannerListener {
    fun onTransactionAccepted(message: String)
    fun onError(message: String)
}
