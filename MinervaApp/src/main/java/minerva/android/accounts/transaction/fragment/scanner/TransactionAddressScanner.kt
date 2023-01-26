package minerva.android.accounts.transaction.fragment.scanner

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.extension.invisible
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.walletmanager.model.walletconnect.WalletConnectUriUtils

class TransactionAddressScanner : BaseScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onCloseButtonAction() {
        listener.onBackPressed()
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun onCallbackAction(qrCode: String) {
        if (WalletConnectUriUtils.isValidWalletConnectUri(qrCode)) {
            Toast.makeText(
                context,
                getString(R.string.scan_wallet_connect_qr_message),
                Toast.LENGTH_LONG
            ).show()
            binding.scannerProgressBar.invisible()
            return
        }
        listener.setScanResult(AddressParser.parse(qrCode))
        shouldScan = false
    }

    companion object {
        @JvmStatic
        fun newInstance(index: Int = Int.InvalidValue) =
            TransactionAddressScanner().apply {
                arguments = Bundle().apply { putInt(ACCOUNT_INDEX, index) }
            }
    }
}
