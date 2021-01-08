package minerva.android.services.login.scanner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import kotlinx.android.synthetic.main.fragment_scanner.*
import minerva.android.R
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.login.LoginScannerListener
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.EncodingJwtFailedThrowable
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.model.ServiceQrCode
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.Exception

class LoginScannerFragment : BaseScannerFragment() {

    private val viewModel: LoginScannerViewModel by viewModel()
    lateinit var listener: LoginScannerListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareObserver()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as LoginScannerListener
    }

    override fun setupCallbacks() {
        codeScanner.apply {
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        scanner_progress_bar.visible()
                        viewModel.validateResult(it.text)
                        shouldScan = false
                    }
                }
            }

            errorCallback = ErrorCallback { handleCameraError(it) }
        }
    }

    private fun prepareObserver() {
        viewModel.apply {
            handleServiceQrCodeLiveData.observe(viewLifecycleOwner, EventObserver { goToChooseIdentityFragment(it) })
            bindCredentialSuccessLiveData.observe(viewLifecycleOwner, EventObserver { listener.onScannerResult(true, it) })
            knownUserLoginLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onPainlessLoginResult(false, payload = it) })
            scannerErrorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onScannerResult(false, getErrorMessage(it)) })
            bindCredentialErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                listener.onScannerResult(false, getErrorMessage(it))
            })
            updateBindedCredential.observe(
                viewLifecycleOwner,
                EventObserver { listener.updateBindedCredential(it) })
        }
    }

    private fun getErrorMessage(it: Throwable): String =
        when (it) {
            is NoBindedCredentialThrowable -> getString(R.string.attached_credential_failure)
            is AutomaticBackupFailedThrowable -> getString(R.string.automatic_backup_failed_error)
            is EncodingJwtFailedThrowable -> getString(R.string.invalid_signature_error_message)
            else -> getString(R.string.unexpected_error)

        }

    private fun goToChooseIdentityFragment(qrCodeCode: ServiceQrCode) {
        Handler().postDelayed({ listener.showChooseIdentityFragment(qrCodeCode) }, DELAY)
    }

    override fun setOnCloseButtonListener() {
        close_button.setOnClickListener {
            listener.onBackPressed()
        }
    }

    override fun onPermissionNotGranted() {
        listener.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun newInstance() = LoginScannerFragment()
    }
}
