package minerva.android.values.listener

interface ScannerFragmentsListener: BaseScannerListener {
    fun showScanner()
    fun setScanResult(text: String?)
}