package minerva.android.accounts.transaction.fragment.scanner

import android.content.Context
import android.widget.Toast
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment
import minerva.android.extension.invisible
import minerva.android.extension.visible

class AddressScannerFragment : WalletConnectScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun setupCallbacks() {
        codeScanner.apply {
            decodeCallback = DecodeCallback { result ->
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        binding.scannerProgressBar.visible()
                        handleScanResult(AddressParser.parse(result.text))
                    }
                }
            }

            errorCallback = ErrorCallback { handleCameraError(it) }
        }
    }

    private fun handleScanResult(parsedResult: String) {
        if (parsedResult == WALLET_CONNECT) {
            Toast.makeText(context, getString(R.string.scan_wallet_connect_qr_message), Toast.LENGTH_LONG).show()
            binding.scannerProgressBar.invisible()
        } else {
            listener.setScanResult(parsedResult)
            shouldScan = false
        }
    }

    override fun setOnCloseButtonListener() {
        binding.closeButton.setOnClickListener {
            listener.onBackPressed()
        }
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddressScannerFragment()
    }
}
