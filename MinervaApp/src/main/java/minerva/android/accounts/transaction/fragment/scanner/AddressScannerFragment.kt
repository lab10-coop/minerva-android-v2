package minerva.android.accounts.transaction.fragment.scanner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import minerva.android.R
import minerva.android.accounts.akm.SafeAccountSettingsFragment
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX
import minerva.android.wrapped.WrappedActivity

class AddressScannerFragment : WalletConnectScannerFragment() {

    private lateinit var listener: AddressScannerListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getInt(ACCOUNT_INDEX)?.let {
            if (it != Int.InvalidValue) viewModel.getAccount(it)
        }
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
        fun newInstance(index: Int = Int.InvalidValue) =
            AddressScannerFragment().apply {
                arguments = Bundle().apply { putInt(ACCOUNT_INDEX, index) }
            }
    }
}
