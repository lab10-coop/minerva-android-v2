package minerva.android.services.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.activity_scanner.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.services.login.PainlessLoginActivity.Companion.SCAN_RESULT
import minerva.wrapped.WrappedActivity

class LiveScannerActivity : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner
    private var isPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        hideToolbar()
        setupScanner()
        setOnCloseButtonListener()
        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionGranted) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun checkCameraPermission() {
        if (isCameraPermissionGranted()) {
            isPermissionGranted = true
        } else {
            isPermissionGranted = false
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun setupScanner() {
        CodeScanner(this@LiveScannerActivity, scanner).apply {
            codeScanner = this
            scanMode = ScanMode.CONTINUOUS
            formats = listOf(BarcodeFormat.QR_CODE)
            autoFocusMode = AutoFocusMode.SAFE
            decodeCallback = DecodeCallback {
                runOnUiThread {
                    launchActivity<PainlessLoginActivity> {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(SCAN_RESULT, it.text)
                    }
                }
            }
            errorCallback = ErrorCallback {
                runOnUiThread {
                    Toast.makeText(this@LiveScannerActivity, "${getString(R.string.camera_error)} ${it.message}", Toast.LENGTH_LONG).show()
                    onBackPressed()
                }
            }
        }
    }

    private fun hideToolbar() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun setOnCloseButtonListener() {
        closeButton.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            isPermissionGranted = if (isCameraPermissionGranted()) {
                codeScanner.startPreview()
                true
            } else {
                false
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 10
    }
}
