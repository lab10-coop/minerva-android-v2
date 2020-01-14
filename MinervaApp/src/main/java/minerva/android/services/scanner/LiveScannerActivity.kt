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
import minerva.android.extension.gone
import minerva.android.extension.launchActivity
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.services.login.PainlessLoginActivity.Companion.SCAN_RESULT
import minerva.android.walletmanager.model.QrCodeResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class LiveScannerActivity : AppCompatActivity() {

    private val viewModel: LiveScannerViewModel by viewModel()

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
        prepareObserver()
        if (isPermissionGranted) {
            codeScanner.startPreview()
        }
    }

    private fun prepareObserver() {
        viewModel.scannerResultLiveData.observe(this, EventObserver { goToPainlessLoginActivity(it) })
        viewModel.scannerErrorLiveData.observe(this, EventObserver { handleError() })
    }

    private fun goToPainlessLoginActivity(it: QrCodeResponse) {
        scannerProgressBar.gone()
        launchActivity<PainlessLoginActivity> {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SCAN_RESULT, it)
        }
        finish()
    }

    private fun handleError() {
        scannerProgressBar.gone()
        Toast.makeText(this@LiveScannerActivity, getString(R.string.invalid_qr_code_message), Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        viewModel.onPause()
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
                    scannerProgressBar.visible()
                    viewModel.validateResult(it.text)
                }
            }

            errorCallback = ErrorCallback {
                runOnUiThread {
                    Toast.makeText(this@LiveScannerActivity, "${getString(R.string.camera_error)} ${it.message}", Toast.LENGTH_LONG).show()
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
