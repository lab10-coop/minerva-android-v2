package minerva.android.values.listener

interface AddressFragmentsListener: BaseScannerListener {
    fun showScanner()
    fun setScanResult(text: String?)
}