package minerva.android.accounts.listener

interface ScannerFragmentsListener: BaseScannerListener {
    fun showScanner()
    fun setScanResult(text: String?)
}