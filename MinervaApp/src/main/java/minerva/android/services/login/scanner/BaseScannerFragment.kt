package minerva.android.services.login.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.BarcodeFormat
import minerva.android.R
import minerva.android.databinding.FragmentScannerBinding
import minerva.android.extension.visible
import minerva.android.kotlinUtils.function.orElse
import java.lang.Exception

abstract class BaseScannerFragment : Fragment(R.layout.fragment_scanner) {

    lateinit var binding: FragmentScannerBinding
    private var isPermissionGranted = false
    var shouldScan = true
    lateinit var codeScanner: CodeScanner
    abstract fun onCloseButtonAction()
    abstract fun onPermissionNotGranted()
    abstract fun setupCallbacks()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScannerBinding.bind(view)
        setupCodeScanner()
        setupCallbacks()
        checkCameraPermission()
        setOnCloseButtonAction()
    }

    override fun onResume() {
        super.onResume()
        shouldScan = true
        startCameraPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    fun setupCallbackAction(action: (text: String) -> Unit) {
        codeScanner.apply {
            decodeCallback = DecodeCallback { result ->
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        binding.scannerProgressBar.visible()
                        setupCallbacks()
                        action(result.text)
                    }
                }
            }
            errorCallback = ErrorCallback { handleCameraError(it) }
        }
    }

    private fun setOnCloseButtonAction() {
        binding.closeButton.setOnClickListener { onCloseButtonAction() }
    }

    private fun setupCodeScanner() {
        CodeScanner(requireActivity(), binding.scanner).apply {
            codeScanner = this
            scanMode = ScanMode.CONTINUOUS
            formats = listOf(BarcodeFormat.QR_CODE)
            autoFocusMode = AutoFocusMode.SAFE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            isPermissionGranted = if (isCameraPermissionGranted()) {
                codeScanner.startPreview()
                true
            } else {
                onPermissionNotGranted()
                false
            }
        }
    }

    fun handleCameraError(it: Exception) {
        requireActivity().runOnUiThread {
            Toast.makeText(context, "${getString(R.string.camera_error)} ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCameraPreview() {
        //delay to wait when camera is active, enables smooth animations
        Handler().postDelayed({
            if (isPermissionGranted) {
                if (!codeScanner.isPreviewActive) {
                    codeScanner.startPreview()
                }
            }
        }, DELAY)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }.orElse {
            false
        }
    }

    private fun checkCameraPermission() {
        isPermissionGranted = if (isCameraPermissionGranted()) {
            true
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            false
        }
    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 10
        const val DELAY = 600L
    }
}