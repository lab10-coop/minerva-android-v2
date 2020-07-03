package minerva.android.accounts.listener

interface TransactionListener : ScannerFragmentsListener {
    fun onTransactionAccepted(message: String)
    fun onError(message: String)
}
