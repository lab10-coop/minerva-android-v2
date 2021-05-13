package minerva.android.accounts.walletconnect

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hitanshudhawan.spannablestringparser.spannify
import minerva.android.R
import minerva.android.extension.*
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.dialog.walletconnect.DappConfirmationDialog
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class WalletConnectScannerFragment : BaseScannerFragment() {

    internal val viewModel: WalletConnectViewModel by sharedViewModel()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private val dappsAdapter: DappsAdapter by lazy {
        DappsAdapter { peerId -> viewModel.killSession(peerId) }
    }
    private var confirmationDialogDialog: DappConfirmationDialog? = null
    private var errorDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setConnectionStatusFlowable()
        observeViewState()
        showWalletConnectViews()
        setupBottomSheet()
        setupRecycler()
    }

    private fun observeViewState() {
        viewModel.stateLiveData.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is WrongQrCodeState -> handleWrongQrCode()
                is CorrectQrCodeState -> shouldScan = false
                is OnDisconnected -> {
                    if (state.sessionName.isNotEmpty()) {
                        showToast(getString(R.string.dapp_disconnected, state.sessionName))
                    } else {
                        showAlertDialog(getString(R.string.session_connection_error))
                    }
                }
                is ProgressBarState -> binding.walletConnectProgress.root.visibleOrInvisible(state.show)
                is OnSessionRequest -> showConnectionDialog(state.meta, state.network, state.dialogType)
                is UpdateDappsState -> dappsAdapter.updateDapps(state.dapps)
                is HideDappsState -> {
                    with(binding) {
                        dappsBottomSheet.dapps.gone()
                        closeButton.margin(bottom = DEFAULT_MARGIN)
                    }
                }
                is OnSessionDeleted -> showToast(getString(R.string.dapp_deleted))
                is OnGeneralError -> handleError(state.error)
                is OnWalletConnectConnectionError -> handleWalletConnectError(state)
            }
        })
        viewModel.errorLiveData.observe(viewLifecycleOwner, EventObserver { handleError(it) })
    }

    private fun handleWalletConnectError(it: OnWalletConnectConnectionError) {
        confirmationDialogDialog?.dismiss()
        val errorMessage = if (it.sessionName.isEmpty()) {
            getString(R.string.session_connection_error)
        } else {
            getString(
                R.string.active_session_connection_error,
                formatString(it.sessionName)
            ).spannify()
        }
        showAlertDialog(errorMessage)
    }

    private fun formatString(text: String) = "{`$text` < text-style:bold />}"

    private fun showAlertDialog(errorMessage: CharSequence) {
        AlertDialogHandler.showDialog(
            requireContext(),
            getString(R.string.try_again),
            errorMessage
        ) {
            errorDialog?.dismiss()
            shouldScan = true
        }
    }

    private fun handleError(error: Throwable) {
        confirmationDialogDialog?.dismiss()
        shouldScan = true
        showToast(getErrorMessage(error))
    }

    private fun getErrorMessage(it: Throwable) =
        if (it is InvalidAccountThrowable) {
            getString(R.string.invalid_account_message)
        } else {
            it.message
        }

    private fun showToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun handleWrongQrCode() {
        showToast(getString(R.string.scan_wc_qr))
        shouldScan = true
    }

    private fun setupBottomSheet() = with(binding) {
        with(BottomSheetBehavior.from(dappsBottomSheet.dapps)) {
            bottomSheetBehavior = this
            peekHeight = PEEK_HEIGHT
        }
    }

    private fun setupRecycler() {
        binding.dappsBottomSheet.connectedDapps.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dappsAdapter
        }
    }

    private fun showWalletConnectViews() = with(binding) {
        walletConnectToolbar.visible()
        dappsBottomSheet.dapps.visible()
    }

    override fun onCallbackAction(qrCode: String) {
        viewModel.handleQrCode(qrCode)
    }

    override fun showProgress() {
        binding.walletConnectProgress.root.visible()
    }

    private fun showConnectionDialog(meta: WalletConnectPeerMeta, network: String, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog = DappConfirmationDialog(requireContext(),
            {
                viewModel.approveSession(meta)
                binding.dappsBottomSheet.dapps.visible()
                shouldScan = true
                binding.closeButton.margin(bottom = INCREASED_MARGIN)
            },
            {
                viewModel.rejectSession()
                shouldScan = true
            }).apply {
            setOnDismissListener { shouldScan = true }
            setView(meta, network)
            handleNetwork(dialogType)
            show()
        }
    }

    private fun DappConfirmationDialog.handleNetwork(dialogType: WalletConnectAlertType) {
        when(dialogType) {
            WalletConnectAlertType.NO_ALERT -> {}
            WalletConnectAlertType.WARNING -> setWrongNetworkWarning(viewModel.requestedNetwork)
            WalletConnectAlertType.ERROR -> setWrongNetworkMessage(viewModel.requestedNetwork)
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING -> setNotDefinedNetworkWarning()
        }
    }

    override fun onCloseButtonAction() {
        closeScanner()
    }

    override fun onPermissionNotGranted() {
        closeScanner()
    }

    private fun closeScanner() {
        viewModel.closeScanner()
        confirmationDialogDialog = null
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