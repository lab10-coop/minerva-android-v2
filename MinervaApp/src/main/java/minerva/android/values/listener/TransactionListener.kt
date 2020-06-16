package minerva.android.values.listener

interface TransactionListener : ScannerFragmentsListener {
    fun onTransactionAccepted(message: String)
    fun onError(message: String)
}
