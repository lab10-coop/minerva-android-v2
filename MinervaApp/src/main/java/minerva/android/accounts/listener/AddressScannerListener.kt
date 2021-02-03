package minerva.android.accounts.listener

import minerva.android.accounts.transaction.fragment.scanner.AddressScannerFragment
import minerva.android.services.login.scanner.BaseScannerFragment

interface AddressScannerListener : BaseScannerListener {
    fun showScanner(scanner: BaseScannerFragment = AddressScannerFragment.newInstance())
    fun setScanResult(text: String?)
}