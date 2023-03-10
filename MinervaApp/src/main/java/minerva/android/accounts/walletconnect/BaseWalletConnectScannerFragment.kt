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
import minerva.android.walletmanager.model.walletconnect.WalletConnectProposalNamespace
import minerva.android.walletmanager.model.walletconnect.WalletConnectSessionNamespace
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepositoryImpl
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
            updateAccountSpinner(dialogType)
            show()
        }
    }

    // todo: move somewhere else?
    private fun areAllNetworksSupported(numberOfNonEip155Chains: Int, eip155ProposalNamespace: WalletConnectProposalNamespace): Boolean {
        if (numberOfNonEip155Chains > 0) {
            return false
        }
        return viewModel.networks
            .map { "eip155:${it.chainId}" }
            .containsAll(eip155ProposalNamespace.chains)
    }

    // todo: move somewhere else?
    private fun areAllMethodsAndEventsSupported(eip155ProposalNamespace: WalletConnectProposalNamespace): Boolean {
        if (/* todo: check methods */ false) {
            return false
        }
        if (/* todo: check events */ false) {
            return false
        }
        return true
    }

    // todo: implement
    // todo: why is this the same as BaseWalletConnectInteractionsActivity?
    protected fun showConnectionDialogV2(meta: WalletConnectPeerMeta, numberOfNonEip155Chains: Int, eip155ProposalNamespace: WalletConnectProposalNamespace) {
        val networksSupported = areAllNetworksSupported(numberOfNonEip155Chains, eip155ProposalNamespace)
        val methodOrEventSupported = areAllMethodsAndEventsSupported(eip155ProposalNamespace)

        confirmationDialogDialog = DappConfirmationDialogV2(requireContext(),
            {
                val selectedAddress = viewModel.address
                // todo: turn this into a function
                // todo: check if address is checksummed
                val chains = viewModel.networks
                    .map { network -> "eip155:${network.chainId}" }
                val accounts = chains
                    .map { chain -> "$chain:$selectedAddress" }

                // approve session
                val sessionNamespace = WalletConnectSessionNamespace(
                    chains,
                    accounts,
                    methods = eip155ProposalNamespace.methods,
                    events = eip155ProposalNamespace.events
                )
                viewModel.approveSessionV2(meta.proposerPublicKey, sessionNamespace, true)
                binding.closeButton.margin(bottom = INCREASED_MARGIN)
            },
            {
                // reject session
                // not localized because it is not user facing
                var reason = "User rejection"
                if (!networksSupported) {
                    reason = "Network(s) not supported"
                } else if (!methodOrEventSupported) {
                    reason = "Method(s) or Event(s) not supported"
                }
                viewModel.rejectSessionV2(meta.proposerPublicKey, reason, true)
                shouldScan = true
            }
        ).apply {
            // todo: and enable/disable connect button?
            var networkNames = WalletConnectRepositoryImpl.proposalNamespacesToChainNames(eip155ProposalNamespace)
            when {
                numberOfNonEip155Chains == 1 -> networkNames = networkNames + getString(R.string.non_evm_chain)
                numberOfNonEip155Chains > 1 -> networkNames = networkNames + getString(R.string.non_evm_chains)
            }
            setView(
                meta,
                ViewDetailsV2(
                    networkNames,
                    getString(R.string.connect_to_website),
                    getString(R.string.connect)
                ),
                viewModel.networks.size
            )
            var walletConnectV2AlertType = WalletConnectV2AlertType.NO_ALERT
            if (!networksSupported) {
                walletConnectV2AlertType = WalletConnectV2AlertType.UNSUPPORTED_NETWORK_WARNING
            } else if (!methodOrEventSupported) {
                walletConnectV2AlertType = WalletConnectV2AlertType.OTHER_UNSUPPORTED
            }
            setWarnings(walletConnectV2AlertType)

            // set addresses in spinner instead of accounts
            updateAddressSpinner()
            show()
        }
    }

    private fun DappConfirmationDialogV1.updateAccountSpinner(dialogType: WalletConnectAlertType) {
        setupAccountSpinner(viewModel.account.id, viewModel.availableAccounts, dialogType) { account ->
            viewModel.setNewAccount(account)
        }
    }

    // walletconnect 2.0
    private fun DappConfirmationDialogV2.updateAddressSpinner() {
        viewModel.setNewAddress(viewModel.availableAddresses[0].address)
        setupAddressSpinner(viewModel.availableAddresses) { address ->
            viewModel.setNewAddress(address)
        }
    }

    private fun DappConfirmationDialogV1.handleNetwork(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        when (dialogType) {
            WalletConnectAlertType.NO_ALERT -> setNoAlert()
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING ->
                setNotDefinedNetworkWarning(viewModel.availableNetworks, dialogType, Int.InvalidId, network) { chainId ->
                    viewModel.setAccountForSelectedNetwork(chainId)
                    updateAccountSpinner(dialogType)
                }
            WalletConnectAlertType.CHANGE_ACCOUNT_WARNING -> setChangeAccountMessage(network.name)
            WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR -> setNoAvailableAccountMessage(network)
            WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING ->
                setUnsupportedNetworkMessage(network, viewModel.account.chainId, viewModel.areMainNetworksEnabled)
            WalletConnectAlertType.CHANGE_ACCOUNT -> { /* do nothing */ }
            WalletConnectAlertType.CHANGE_NETWORK -> { /* do nothing */ }
        }
    }

    protected fun updateConnectionDialog(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog?.apply {
            if (this is DappConfirmationDialogV1) {
                updateAccountSpinner(dialogType)
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