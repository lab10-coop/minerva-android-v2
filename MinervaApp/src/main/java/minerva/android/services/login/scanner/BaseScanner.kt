package minerva.android.services.login.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.ScanMode
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.fragment_scanner.*
import minerva.android.R
import minerva.android.kotlinUtils.function.orElse

abstract class BaseScanner : Fragment() {
    private var isPermissionGranted = false
    var shouldScan = true
    lateinit var codeScanner: CodeScanner
    abstract fun setOnCloseButtonListener()
    abstract fun onPermissionNotGranted()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCodeScanner()
        checkCameraPermission()
        setOnCloseButtonListener()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_scanner, container, false)

    override fun onResume() {
        super.onResume()
        shouldScan = true
        startCameraPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    open fun setupCodeScanner() {
        CodeScanner(requireActivity(), scanner).apply {
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