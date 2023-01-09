package minerva.android.accounts.walletconnect

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class WalletConnectScannerFragment : BaseWalletConnectScannerFragment() {

    override val viewModel: WalletConnectViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.subscribeToWCConnectionStatusFlowable()
        observeViewState()
    }

    private fun observeViewState() {
        viewModel.stateLiveData.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is WrongWalletConnectCodeState -> handleWrongQrCode()
                is CorrectWalletConnectCodeState -> shouldScan = false
                is OnDisconnected -> handleWalletConnectDisconnectState(state.sessionName)
                is ProgressBarState -> binding.walletConnectProgress.root.visibleOrInvisible(state.show)
                is OnSessionRequest -> showConnectionDialog(state.meta, state.network, state.dialogType)
                is OnSessionRequestV2 -> showConnectionDialogV2(state.meta, state.networkNames)
                is HideDappsState -> {
                    with(binding) {
                        closeButton.margin(bottom = DEFAULT_MARGIN)
                    }
                }
                is OnGeneralError -> handleError(state.error)
                is OnWalletConnectConnectionError -> handleWalletConnectError(state.sessionName)
                is UpdateOnSessionRequest -> updateConnectionDialog(state.network, state.dialogType)
            }
        })
        viewModel.errorLiveData.observe(viewLifecycleOwner, EventObserver { error -> handleError(error) })
    }

    override fun getErrorMessage(error: Throwable) =
        if (error is InvalidAccountThrowable) {
            getString(R.string.invalid_account_message)
        } else {
            error.message ?: getString(R.string.unexpected_error)
        }

    private fun handleWrongQrCode() {
        showToast(getString(R.string.scan_wc_qr))
        shouldScan = true
    }

    override fun onCallbackAction(result: String) {
        viewModel.handleQrCode(result)
    }

    override fun showProgress() {
        binding.walletConnectProgress.root.visible()
    }


    override fun onCloseButtonAction() {
        closeScanner()
    }

    override fun onPermissionNotGranted() {
        closeScanner()
    }

    private fun closeScanner() {
        viewModel.closeScanner()
        clearDialog()
    }

    companion object {
        @JvmStatic
        fun newInstance() = WalletConnectScannerFragment()

        const val PEEK_HEIGHT = 240
        const val DEFAULT_MARGIN = 32f
        const val INCREASED_MARGIN = 115f
        const val FIRST_ICON = 0
    }
}