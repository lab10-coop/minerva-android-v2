package minerva.android.accounts.listener

import minerva.android.accounts.transaction.fragment.scanner.TransactionAddressScanner
import minerva.android.services.login.scanner.BaseScannerFragment

interface AddressScannerListener : BaseScannerListener {
    fun showScanner(scanner: BaseScannerFragment = TransactionAddressScanner.newInstance())
    fun setScanResult(text: String?)
}