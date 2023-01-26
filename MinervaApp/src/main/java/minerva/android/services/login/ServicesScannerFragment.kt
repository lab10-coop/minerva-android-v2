package minerva.android.services.login

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerFragment
import minerva.android.extension.empty
import minerva.android.extension.visibleOrInvisible
import minerva.android.walletmanager.model.ServiceQrCode
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesScannerFragment : BaseWalletConnectScannerFragment() {

    override val viewModel: ServicesScannerViewModel by viewModel()
    lateinit var listener: ServicesScannerListener

    override fun onCloseButtonAction() {
        clearDialog()
        listener.onBackPressed()
    }

    override fun onPermissionNotGranted() {
        clearDialog()
        listener.onBackPressed()
    }

    override fun onCallbackAction(result: String) {
        viewModel.validateResult(result)
        shouldScan = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.subscribeToWCConnectionStatusFlowable()
        prepareObserver()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as ServicesScannerListener
    }

    private fun prepareObserver() {
        viewModel.apply {
            viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is ServiceLoginResult -> goToChooseIdentityFragment(state.qrCode)
                    is CredentialsLoginResult -> {
                        clearDialog()
                        listener.onScannerResult(true, state.message)
                    }
                    CloseScannerState -> {
                        clearDialog()
                        listener.onScannerResult(true, String.empty, true)
                    }
                    is UpdateCredentialsLoginResult -> {
                        clearDialog()
                        listener.updateBindedCredential(state.qrCode)
                    }
                    is Error -> handleError(state.error)
                    is ProgressBarState -> binding.scannerProgressBar.visibleOrInvisible(state.isVisible)
                    CorrectWalletConnectResult -> {
                        hideProgress()
                        shouldScan = false
                    }
                    is WalletConnectSessionRequestResult -> showConnectionDialog(state.meta, state.network, state.dialogType)
                    is WalletConnectSessionRequestResultV2 -> showConnectionDialogV2(state.meta, state.networkNames)
                    is WalletConnectUpdateDataState -> updateConnectionDialog(state.network, state.dialogType)
                    is WalletConnectDisconnectResult -> handleWalletConnectDisconnectState(state.sessionName)
                    is WalletConnectConnectionError -> handleWalletConnectError(state.sessionName)
                    DefaultState -> {
                    }
                }
            })
        }
    }

    override fun getErrorMessage(error: Throwable): String = error.message ?: getString(R.string.unexpected_error)

    private fun goToChooseIdentityFragment(qrCodeCode: ServiceQrCode) {
        hideProgress()
        Handler().postDelayed({ listener.showChooseIdentityFragment(qrCodeCode) }, DELAY)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ServicesScannerFragment()
    }

}