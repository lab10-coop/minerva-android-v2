package minerva.android.accounts.transaction.fragment.scanner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import minerva.android.R
import minerva.android.accounts.listener.AddressScannerListener
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerFragment
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerViewModel
import minerva.android.extension.invisible
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity.Companion.ACCOUNT_INDEX
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

// todo: why is this based on the BaseWalletConnectScannerFragment?
class TransactionAddressScanner : BaseWalletConnectScannerFragment() {

    // todo: not sure why this viewModel is even needed
    override val viewModel: BaseWalletConnectScannerViewModel by sharedViewModel()
    private lateinit var listener: AddressScannerListener

    override fun onCloseButtonAction() {
        listener.onBackPressed()
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // todo: this line seem to be useless
        //arguments?.getInt(ACCOUNT_INDEX)?.let { viewModel.getAccount(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressScannerListener
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

    override fun getErrorMessage(error: Throwable) =
        if (error is InvalidAccountThrowable) {
            getString(R.string.invalid_account_message)
        } else {
            error.message ?: getString(R.string.unexpected_error)
        }

    companion object {
        @JvmStatic
        fun newInstance(index: Int = Int.InvalidValue) =
            TransactionAddressScanner().apply {
                arguments = Bundle().apply { putInt(ACCOUNT_INDEX, index) }
            }
    }
}
