package minerva.android.accounts.transaction.fragment.scanner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment
import minerva.android.extension.invisible
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX

class AddressScannerFragment : WalletConnectScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getInt(ACCOUNT_INDEX)?.let { viewModel.getAccount(it) }
    }

    override fun onCallbackAction(qrCode: String) {
        handleScanResult(AddressParser.parse(qrCode))
    }

    private fun handleScanResult(parsedResult: String) {
        if (parsedResult == WALLET_CONNECT) {
            Toast.makeText(
                context,
                getString(R.string.scan_wallet_connect_qr_message),
                Toast.LENGTH_LONG
            ).show()
            binding.scannerProgressBar.invisible()
        } else {
            listener.setScanResult(parsedResult)
            shouldScan = false
        }
    }

    override fun onCloseButtonAction() {
        listener.onBackPressed()
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun newInstance(index: Int = Int.InvalidValue) =
            AddressScannerFragment().apply {
                arguments = Bundle().apply { putInt(ACCOUNT_INDEX, index) }
            }
    }
}
