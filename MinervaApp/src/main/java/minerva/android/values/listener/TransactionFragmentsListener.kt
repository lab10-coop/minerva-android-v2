package minerva.android.values.listener

interface TransactionFragmentsListener: BaseScannerListener {
    fun showScanner()
    fun setScanResult(text: String?)
}