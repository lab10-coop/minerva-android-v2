package minerva.android.values.akm

import android.content.Context
import android.widget.Toast
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import kotlinx.android.synthetic.main.fragment_scanner.*
import minerva.android.R
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScanner
import minerva.android.wrapped.WrappedActivityListener

//TODO need to be refactored - code duplication with TransactionScannerFragment
class AddressScannerFragment : BaseScanner() {

    private lateinit var listener: WrappedActivityListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as WrappedActivityListener
    }

    override fun setupCodeScanner() {
        super.setupCodeScanner()
        codeScanner.apply {
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        scannerProgressBar.visible()
                        shouldScan = false
                        listener.putStringExtra(it.text)
                        listener.goBack(this@AddressScannerFragment)
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
            listener.goBack(this)
        }
    }

    override fun onPermissionNotGranted() {
        listener.goBack(this)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddressScannerFragment()
        const val SCANNER_FRAGMENT = "scannerFragment"
    }

}