package minerva.android.accounts.listener

interface AddressScannerListener : BaseScannerListener {
    fun showScanner()
    fun setScanResult(text: String?)
}