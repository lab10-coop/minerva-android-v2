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
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.login.PainlessLoginFragmentListener
import minerva.android.walletmanager.model.QrCodeResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginScannerFragment : BaseScanner() {

    private val viewModel: LoginScannerViewModel by viewModel()
    lateinit var listener: PainlessLoginFragmentListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareObserver()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as PainlessLoginFragmentListener
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
            scannerResultLiveData.observe(viewLifecycleOwner, EventObserver { goToChooseIdentityFragment(it) })
            scannerErrorLiveData.observe(viewLifecycleOwner, EventObserver { handleError() })
            knownUserLoginLiveData.observe(viewLifecycleOwner, EventObserver {
                listener.onResult(false, loginPayload = it)
            })
        }
    }

    private fun goToChooseIdentityFragment(qrCodeResponse: QrCodeResponse) {
        Handler().postDelayed({ listener.showChooseIdentityFragment(qrCodeResponse) }, DELAY)
    }

    private fun handleError() {
        scannerProgressBar.gone()
        Toast.makeText(context, getString(R.string.invalid_qr_code_message), Toast.LENGTH_LONG).show()
        shouldScan = true
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
