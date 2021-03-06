package minerva.android.values.transaction.fragment.scanner


import android.content.Context
import android.widget.Toast
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import kotlinx.android.synthetic.main.fragment_scanner.*
import minerva.android.R
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScanner
import minerva.android.values.listener.AddressFragmentsListener

//TODO need to be refactored - code duplication with AddressScannerFragment
class TransactionScannerFragment : BaseScanner() {

    private lateinit var listener: AddressFragmentsListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as AddressFragmentsListener
    }

    override fun setupCodeScanner() {
        super.setupCodeScanner()
        codeScanner.apply {
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        scannerProgressBar.visible()
                        listener.setScanResult(it.text)
                        shouldScan = false
                    }
                }
            }

            errorCallback = ErrorCallback {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "${getString(R.string.camera_error)} ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun setOnCloseButtonListener() {
        closeButton.setOnClickListener {
            listener.onBackPressed()
        }
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun newInstance() = TransactionScannerFragment()
    }
}
