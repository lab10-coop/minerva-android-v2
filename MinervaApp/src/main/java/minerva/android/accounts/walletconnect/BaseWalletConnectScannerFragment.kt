package minerva.android.accounts.walletconnect

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.hitanshudhawan.spannablestringparser.spannify
import minerva.android.R
import minerva.android.extension.margin
import minerva.android.kotlinUtils.InvalidId
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.dialog.models.ViewDetails
import minerva.android.widget.dialog.models.ViewDetailsV2
import minerva.android.widget.dialog.walletconnect.DappConfirmationDialogV1
import minerva.android.widget.dialog.walletconnect.DappConfirmationDialogV2
import minerva.android.widget.dialog.walletconnect.DappDialog

abstract class BaseWalletConnectScannerFragment : BaseScannerFragment() {

    abstract val viewModel: BaseWalletConnectScannerViewModel
    private var confirmationDialogDialog: DappDialog?  = null // todo: is DappDialog to broad?
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
        showToast(
            getString(
                R.string.dapp_disconnected,
                if (sessionName.isNotEmpty()) sessionName else getString(R.string.dapp_unnamed)
            )
        )
    }

    protected fun showConnectionDialog(meta: WalletConnectPeerMeta, network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        //set view details for alert dialog
        val viewDetails: ViewDetails = if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType) { //change connection case
            ViewDetails(network.name, getString(R.string.change_account_dialog), getString(R.string.change))
        } else { // connect connection case
            ViewDetails(network.name, getString(R.string.connect_to_website), getString(R.string.connect))
        }
        confirmationDialogDialog = DappConfirmationDialogV1(requireContext(),
            {
                viewModel.approveSession(meta, false)
                binding.closeButton.margin(bottom = INCREASED_MARGIN)
            },
            {
                viewModel.rejectSession()
                shouldScan = true
            },
            { chainId ->
                viewModel.addAccount(chainId, dialogType)
            }
        )
        .apply {
            setOnDismissListener { shouldScan = true }
            setView(meta, viewDetails)
            handleNetwork(network, dialogType)
            updateAccountSpinner()
            show()
        }
    }

    protected fun showConnectionDialogV2(meta: WalletConnectPeerMeta, networkNames: List<String>) {
        confirmationDialogDialog = DappConfirmationDialogV2(requireContext(),
            {
                // todo: approve session
                binding.closeButton.margin(bottom = INCREASED_MARGIN)
            },
            {
                // todo: reject session
                shouldScan = true
            }
        ).apply {
            setOnDismissListener { shouldScan = true }
            setView(
                meta,
                ViewDetailsV2(
                    networkNames,
                    getString(R.string.connect_to_website), getString(R.string.connect)
                )
            )
            //handleNetwork(network, dialogType, meta) // todo?
            //updateAccountSpinner() // todo?
            show()
        }
    }

    private fun DappConfirmationDialogV1.updateAccountSpinner() {
        setupAccountSpinner(viewModel.account.id, viewModel.availableAccounts) { account ->
            viewModel.setNewAccount(account)
        }
    }

    private fun DappConfirmationDialogV1.handleNetwork(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        when (dialogType) {
            WalletConnectAlertType.NO_ALERT -> setNoAlert()
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING -> setNotDefinedNetworkWarning(viewModel.availableNetworks, dialogType, Int.InvalidId) { chainId ->
                viewModel.setAccountForSelectedNetwork(chainId)
                updateAccountSpinner()
            }
            WalletConnectAlertType.CHANGE_ACCOUNT_WARNING -> setChangeAccountMessage(network.name)
            WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR -> setNoAvailableAccountMessage(network)
            WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING -> setUnsupportedNetworkMessage(network, viewModel.account.chainId)
        }
    }

    protected fun updateConnectionDialog(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog?.apply {
            if (this is DappConfirmationDialogV1) {
                updateAccountSpinner()
                handleNetwork(network, dialogType)
            }
        }
    }

    protected fun clearDialog() {
        hideProgress()
        confirmationDialogDialog = null
    }

    companion object {
        const val INCREASED_MARGIN = 115f
        const val FIRST_ICON = 0
    }
}