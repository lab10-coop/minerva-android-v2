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
import minerva.android.walletmanager.model.ServiceQrCode
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginScannerFragment : BaseScanner() {

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

    override fun setupCodeScanner() {
        super.setupCodeScanner()
        codeScanner.apply {
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        scannerProgressBar.visible()
                        viewModel.validateResult(it.text)
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

    private fun prepareObserver() {
        viewModel.apply {
            handleServiceQrCodeLiveData.observe(viewLifecycleOwner, EventObserver { goToChooseIdentityFragment(it) })
            scannerErrorLiveData.observe(viewLifecycleOwner, EventObserver { listener.onScannerResult(false, getString(R.string.invalid_signature_error_message)) })
            knownUserLoginLiveData.observe(viewLifecycleOwner, EventObserver { listener.onPainlessLoginResult(false, payload = it) })
            bindCredentialSuccessLiveData.observe(viewLifecycleOwner, EventObserver { listener.onScannerResult(true, it) })
            bindCredentialErrorLiveData.observe(
                viewLifecycleOwner,
                EventObserver { listener.onScannerResult(false, getString(R.string.attached_credential_failure)) })
            updateBindedCredential.observe(viewLifecycleOwner, EventObserver { listener.updateBindedCredential(it) })
        }
    }

    private fun goToChooseIdentityFragment(qrCodeCode: ServiceQrCode) {
        Handler().postDelayed({ listener.showChooseIdentityFragment(qrCodeCode) }, DELAY)
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
        fun newInstance() = LoginScannerFragment()
    }
}
