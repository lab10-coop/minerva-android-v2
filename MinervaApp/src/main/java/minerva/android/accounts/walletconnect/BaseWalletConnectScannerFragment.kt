package minerva.android.accounts.walletconnect

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.hitanshudhawan.spannablestringparser.spannify
import minerva.android.R
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.dialog.walletconnect.DappConfirmationDialog

abstract class BaseWalletConnectScannerFragment : BaseScannerFragment() {

    abstract val viewModel: BaseWalletConnectScannerViewModel
    private var confirmationDialogDialog: DappConfirmationDialog? = null
    private var errorDialog: AlertDialog? = null

    abstract fun getErrorMessage(error: Throwable): String?

    protected fun handleWalletConnectError(sessionName: String) {
        confirmationDialogDialog?.dismiss()
        val errorMessage = if (sessionName.isEmpty()) {
            getString(R.string.session_connection_error)
        } else {
            getString(
                R.string.active_session_connection_error,
                formatString(sessionName)
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

    protected fun handleError(error: Throwable) {
        hideProgress()
        confirmationDialogDialog?.dismiss()
        shouldScan = true
        showToast(getErrorMessage(error))
    }

    protected fun showToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    protected fun handleWalletConnectDisconnectState(sessionName: String) {
        if (sessionName.isNotEmpty()) {
            showToast(getString(R.string.dapp_disconnected, sessionName))
        } else {
            showAlertDialog(getString(R.string.session_connection_error))
        }
    }

    protected fun showConnectionDialog(meta: WalletConnectPeerMeta, network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog = DappConfirmationDialog(requireContext(),
            {
                viewModel.approveSession(meta, false)
                binding.dappsBottomSheet.dapps.visible()
                binding.closeButton.margin(bottom = WalletConnectScannerFragment.INCREASED_MARGIN)
            },
            {
                viewModel.rejectSession()
                shouldScan = true
            },
            { chainId ->
                viewModel.addAccount(chainId, dialogType)
            }).apply {
            setOnDismissListener { shouldScan = true }
            setView(meta, network.name)
            handleNetwork(network, dialogType)
            updateAccountSpinner()
            show()
        }
    }

    private fun DappConfirmationDialog.updateAccountSpinner() {
        setupAccountSpinner(viewModel.account.id, viewModel.availableAccounts) { account ->
            viewModel.setNewAccount(account)
        }
    }

    private fun DappConfirmationDialog.handleNetwork(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        when (dialogType) {
            WalletConnectAlertType.NO_ALERT -> setNoAlert()
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING -> setNotDefinedNetworkWarning(viewModel.availableNetworks) { chainId ->
                viewModel.setAccountForSelectedNetwork(chainId)
                updateAccountSpinner()
            }
            WalletConnectAlertType.CHANGE_ACCOUNT_WARNING -> setChangeAccountMessage(network.name)
            WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR -> setNoAvailableAccountMessage(network)
            WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING -> setUnsupportedNetworkMessage(network)
        }
    }

    protected fun updateConnectionDialog(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog?.apply {
            updateAccountSpinner()
            handleNetwork(network, dialogType)
        }
    }

    protected fun clearDialog() {
        hideProgress()
        confirmationDialogDialog = null
    }
}