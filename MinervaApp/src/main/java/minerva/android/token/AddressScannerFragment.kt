package minerva.android.token

import android.os.Bundle
import android.view.View
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.services.login.scanner.BaseScannerFragment

class AddressScannerFragment : BaseScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener = context as AddressScannerListener
    }

    override fun onCloseButtonAction() {
        listener.onBackPressed()
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    override fun onCallbackAction(address: String) {
        listener.setScanResult(address)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddressScannerFragment()
    }
}