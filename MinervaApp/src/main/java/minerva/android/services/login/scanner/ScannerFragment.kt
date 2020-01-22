package minerva.android.services.login.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.*
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.fragment_scanner.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.services.login.PainlessLoginFragmentListener
import minerva.android.walletmanager.model.QrCodeResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class ScannerFragment : Fragment() {

    private val viewModel: ScannerViewModel by viewModel()

    private lateinit var listener: PainlessLoginFragmentListener
    private lateinit var codeScanner: CodeScanner
    private var isPermissionGranted = false
    private var shouldScan = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PainlessLoginFragmentListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareObserver()
        setupScanner()
        checkCameraPermission()
        setOnCloseButtonListener()
    }

    override fun onResume() {
        super.onResume()
        shouldScan = true
        startCameraPreview()
    }

    private fun startCameraPreview() {
        //        delay to wait when camera is active, enables smooth animations
        Handler().postDelayed({
            if (isPermissionGranted) {
                if (!codeScanner.isPreviewActive) {
                    codeScanner.startPreview()
                }
            }
        }, DELAY)
    }

    override fun onPause() {
        codeScanner.releaseResources()
        viewModel.onPause()
        super.onPause()
    }

    private fun setupScanner() {
        val activity = requireActivity()
        CodeScanner(activity, scanner).apply {
            codeScanner = this
            scanMode = ScanMode.CONTINUOUS
            formats = listOf(BarcodeFormat.QR_CODE)
            autoFocusMode = AutoFocusMode.SAFE

            decodeCallback = DecodeCallback {
                activity.runOnUiThread {
                    if (shouldScan) {
                        scannerProgressBar.visible()
                        viewModel.validateResult(it.text)
                        shouldScan = false
                    }
                }
            }

            errorCallback = ErrorCallback {
                activity.runOnUiThread {
                    Toast.makeText(context, "${getString(R.string.camera_error)} ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun prepareObserver() {
        viewModel.scannerResultLiveData.observe(this, EventObserver { goToChooseIdentityFragment(it) })
        viewModel.scannerErrorLiveData.observe(this, EventObserver { handleError() })
    }

    private fun goToChooseIdentityFragment(qrCodeResponse: QrCodeResponse) {
        Handler().postDelayed({
            listener.showChooseIdentityFragment(qrCodeResponse)
        }, DELAY)
    }

    private fun handleError() {
        scannerProgressBar.gone()
        Toast.makeText(context, getString(R.string.invalid_qr_code_message), Toast.LENGTH_LONG).show()
        shouldScan = true
    }

    private fun checkCameraPermission() {
        isPermissionGranted = if (isCameraPermissionGranted()) {
            true
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            false
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }.orElse {
            false
        }
    }

    private fun setOnCloseButtonListener() {
        closeButton.setOnClickListener {
            listener.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            isPermissionGranted = if (isCameraPermissionGranted()) {
                codeScanner?.startPreview()
                true
            } else {
                listener.onBackPressed()
                false
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScannerFragment()

        private const val REQUEST_CAMERA_PERMISSION = 10
        private const val DELAY = 600L
        val TAG: String = this::class.java.canonicalName ?: "ScannerFragment"
    }
}
