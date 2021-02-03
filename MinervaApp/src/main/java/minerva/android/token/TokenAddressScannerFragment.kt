package minerva.android.token

import android.os.Bundle
import android.view.View
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.services.login.scanner.BaseScannerFragment

class TokenAddressScannerFragment : BaseScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener = context as AddressScannerListener
    }

    override fun setOnCloseButtonListener() {
        setOnCloseButtonAction {
            listener.onBackPressed()
        }
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    override fun setupCallbacks() {
        setupCallbackAction { address -> listener.setScanResult(address) }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TokenAddressScannerFragment()
    }
}