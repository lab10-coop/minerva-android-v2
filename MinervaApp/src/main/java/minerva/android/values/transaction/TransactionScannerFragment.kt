package minerva.android.values.transaction


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.fragment_scanner.*

import minerva.android.R
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScanner
import minerva.android.services.login.scanner.LoginScannerFragment
import minerva.android.values.listener.TransactionFragmentsListener


class TransactionScannerFragment : BaseScanner() {

    private lateinit var listener: TransactionFragmentsListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as TransactionFragmentsListener
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
