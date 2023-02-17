package minerva.android.walletConnect

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.firebase.iid.FirebaseInstanceId
import com.hitanshudhawan.spannablestringparser.spannify
import minerva.android.R
import minerva.android.accounts.walletconnect.*
import minerva.android.extension.empty
import minerva.android.extension.getCurrentFragment
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.MainActivity
import minerva.android.main.handler.replaceFragment
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.services.ServicesFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectProposalNamespace
import minerva.android.walletmanager.model.walletconnect.WalletConnectSessionNamespace
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepositoryImpl
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.MinervaLoadingDialog
import minerva.android.widget.dialog.models.ViewDetails
import minerva.android.widget.dialog.models.ViewDetailsV2
import minerva.android.widget.dialog.walletconnect.*
import org.koin.android.ext.android.inject
import java.math.BigDecimal

abstract class BaseWalletConnectInteractionsActivity : AppCompatActivity() {

    private val viewModel: WalletConnectInteractionsViewModel by inject()

    private var dappDialog: DappDialog? = null
    private var loadingDialog: MinervaLoadingDialog? = null
    private var confirmationDialogDialog: DappDialog?  = null // todo: is DappDialog too broad?
    private var errorDialog: AlertDialog? = null

    abstract fun isProtectTransactionEnabled(): Boolean

    /**
     * On Change Account - method which tries to change state of viewModel
     * @param state - new (viewModel::_walletConnectStatus) state
     */
    fun onChangeAccount(state: WalletConnectState) {
        viewModel.onChangeAccount(state)
    }

    private fun rejectRequest() {
        dappDialog?.let {
            it.dismiss()
            viewModel.rejectRequest(true)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.dispose()
        rejectRequest()
    }

    override fun onNewIntent(intent: Intent?) {
        handleDeepLink(intent)
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearAllDialogs()
    }

    override fun onResume() {
        super.onResume()
        with(viewModel) {
            getWalletConnectSessions()
            subscribeToWCConnectionStatusFlowable()
        }
    }

    protected fun prepareWalletConnect() {
        prepareWalletConnectInteractionObservers()
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.getParcelableExtra<Uri>(MOBILE_WALLET_CONNECT_DATA)?.let {
            handleWCConnectionDeepLink(it.toString())
        }
    }

    private fun prepareWalletConnectInteractionObservers() {
        viewModel.walletConnectStatus.observe(this@BaseWalletConnectInteractionsActivity) { state ->
            dismissDialogs()
            when (state) {
                is OnEthSignRequest -> dappDialog = getDappSignDialog(state)
                is OnEthSignRequestV2 -> dappDialog = getDappSignDialogV2(state)
                is OnEthSendTransactionRequest -> dappDialog = getSendTransactionDialog(state)
                is ProgressBarState -> handleLoadingDialog(state)
                is OnGeneralError -> handleWalletConnectGeneralError(state.error.message)
                is OnWalletConnectTransactionError -> handleWalletConnectTransactionError(state)
                is WrongTransactionValueState -> handleWrongTransactionValueState(state)
                is OnDisconnected -> handleWalletConnectDisconnectState(state.sessionName)
                is OnWalletConnectConnectionError -> handleWalletConnectError(state.sessionName)
                is OnSessionRequest -> showConnectionDialog(state.meta, state.network, state.dialogType)
                is OnSessionRequestV2 -> showConnectionDialogV2(state.meta, state.numberOfNonEip155Chains, state.eip155ProposalNamespace)
                is UpdateOnSessionRequest -> updateConnectionDialog(state.network, state.dialogType)
                CloseScannerState -> closeToBackground()
                CloseDialogState -> closeDialog()
                CorrectWalletConnectCodeState -> handleLoadingDialogForWCConnection(true)
                //navigate to ServicesFragment
                ToServiceFragmentRequest -> (this as MainActivity).replaceFragment(ServicesFragment.newInstance())
                else -> {}
            }
        }
        viewModel.errorLiveData.observe(this, EventObserver { handleWalletConnectGeneralError(it.message) })
    }

    private fun handleWalletConnectTransactionError(state: OnWalletConnectTransactionError) {
        dappDialog?.dismiss()
        showFlashbar(
            getString(R.string.transactions_error_title),
            state.error.message ?: getString(R.string.transactions_error_message)
        )
    }

    private fun handleWrongTransactionValueState(state: WrongTransactionValueState) {
        val firebaseID: String = FirebaseInstanceId.getInstance().id
        viewModel.logToFirebase("Transaction with invalid value: ${state.transaction}, firebaseId: $firebaseID")
        AlertDialogHandler.showDialog(
            this,
            getString(R.string.error_header),
            getString(R.string.wrong_tx_value_error, firebaseID)
        )
    }

    private fun handleWalletConnectGeneralError(message: String?) {
        dappDialog?.dismiss()
        confirmationDialogDialog?.dismiss()
        clearAllDialogs()
        val errorMessage = if (message.isNullOrEmpty()) {
            getString(R.string.wc_connection_error_message)
        } else message
        showFlashbar(getString(R.string.wallet_connect_title), errorMessage)
    }

    private fun handleLoadingDialog(state: ProgressBarState) {
        if (state.show) {
            loadingDialog = MinervaLoadingDialog(this).apply { show() }
        } else {
            loadingDialog?.dismiss()
        }
    }

    private fun handleLoadingDialogForWCConnection(shouldShow: Boolean) {
        if (shouldShow) {
            loadingDialog = MinervaLoadingDialog(this).apply {
                setMessage(R.string.info_awaiting)
                show()
            }
        } else {
            loadingDialog?.dismiss()
        }
    }

    private fun getSendTransactionDialog(txRequest: OnEthSendTransactionRequest) =
        with(viewModel) {
            DappSendTransactionDialog(
                this@BaseWalletConnectInteractionsActivity,
                {
                    if (isProtectTransactionEnabled()) {
                        getCurrentFragment()?.showBiometricPrompt(
                            { sendTransaction(txRequest.session.isMobileWalletConnect) },
                            { rejectRequest(txRequest.session.isMobileWalletConnect) }
                        )
                    } else sendTransaction(txRequest.session.isMobileWalletConnect)
                },
                {
                    rejectRequest(txRequest.session.isMobileWalletConnect)
                }).apply {
                setContent(
                    txRequest.transaction, txRequest.session, txRequest.account,
                    { showGasPriceDialog(txRequest) },
                    { gasPrice -> recalculateTxCost(gasPrice, txRequest.transaction) },
                    { balance, cost -> isBalanceTooLow(balance, cost) }
                )
                show()
            }
        }

    private fun DappSendTransactionDialog.showGasPriceDialog(it: OnEthSendTransactionRequest) {
        GasPriceDialog(context) { gasPrice ->
            setCustomGasPrice(
                viewModel.recalculateTxCost(BigDecimal(gasPrice), it.transaction),
                it.account
            ) { balance, cost -> viewModel.isBalanceTooLow(balance, cost) }
        }.show()
    }

    private fun getDappSignDialog(it: OnEthSignRequest) =
        DappSignMessageDialog(this@BaseWalletConnectInteractionsActivity,
            { viewModel.acceptRequest(it.session.isMobileWalletConnect) },
            { viewModel.rejectRequest(it.session.isMobileWalletConnect) }
        ).apply {
            setContent(it.message, it.session)
            show()
        }

    private fun getDappSignDialogV2(req: OnEthSignRequestV2) =
        DappSignMessageDialog(this@BaseWalletConnectInteractionsActivity,
            { viewModel.acceptRequestV2(req.session) },
            { viewModel.rejectRequestV2(req.session) }
        ).apply {
            setContentV2(req.message, req.session)
            show()
        }

    protected fun showFlashbar(title: String, message: String) {
        MinervaFlashbar.show(this@BaseWalletConnectInteractionsActivity, title, message)
    }

    private fun handleWalletConnectError(sessionName: String) {
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
            this,
            getString(R.string.try_again),
            errorMessage
        ) {
            errorDialog?.dismiss()
        }
    }

    private fun handleWalletConnectDisconnectState(sessionName: String) {
        showFlashbar(
            String.empty,
            getString(
                R.string.dapp_disconnected,
                if (sessionName.isNotEmpty()) sessionName else getString(R.string.dapp_unnamed)
            )
        )
    }

    private fun showConnectionDialog(meta: WalletConnectPeerMeta, network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        //set view details for alert dialog
        val viewDetails: ViewDetails = if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType) { //change connection case
            ViewDetails(network.name, getString(R.string.change_account_dialog), getString(R.string.change))
        } else { // connect connection case
            ViewDetails(network.name, getString(R.string.connect_to_website), getString(R.string.connect))
        }
        confirmationDialogDialog = DappConfirmationDialogV1(this,
            {
                if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType) {
                    viewModel.updateSession(meta.peerId)
                } else {
                    viewModel.approveSession(meta, true)
                }
                clearAllDialogs()
            },
            {
                viewModel.rejectSession()
                clearAllDialogs()
            },
            { chainId ->
                viewModel.addAccount(chainId, dialogType)
            }
        ).apply {
            setView(meta, viewDetails)
            handleNetwork(network, dialogType, meta)
            updateAccountSpinner()
            show()
        }
    }

    // todo: move somewhere else?
    private fun areAllNetworksSupported(numberOfNonEip155Chains: Int, eip155ProposalNamespace: WalletConnectProposalNamespace): Boolean {
        if (numberOfNonEip155Chains > 0) {
            return false
        }
        if (/* todo: unsupported evm chains */ false) {
            return false
        }
        return true
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

    // todo: move somewhere else?
    private fun areAllExtensionsSupported(eip155ProposalNamespace: WalletConnectProposalNamespace): Boolean {
        // todo: check extensions
        return true
    }

    // todo: implement
    // todo: why is this the same as BaseWalletConnectScannerFragment?
    private fun showConnectionDialogV2(meta: WalletConnectPeerMeta, numberOfNonEip155Chains: Int, eip155ProposalNamespace: WalletConnectProposalNamespace) {
        val networksSupported = areAllNetworksSupported(numberOfNonEip155Chains, eip155ProposalNamespace)
        val methodOrEventSupported = areAllMethodsAndEventsSupported(eip155ProposalNamespace)
        val extensionsSupported = areAllExtensionsSupported(eip155ProposalNamespace)

        confirmationDialogDialog = DappConfirmationDialogV2(this,
            {
                // todo: set availableAddresses in confirmationDialog and make the user select one.
                val selectedAddress = viewModel.availableAddresses[0]
                // todo: turn this into a function
                // todo: check if address is checksummed
                val accounts = viewModel.availableNetworks
                    .map { network -> "eip155:" + network.chainId + ":" + selectedAddress }

                // approve session
                val sessionNamespace = WalletConnectSessionNamespace(
                    accounts,
                    methods = eip155ProposalNamespace.methods,
                    events = eip155ProposalNamespace.events,
                    extensions = null // todo
                )
                viewModel.approveSessionV2(meta.proposerPublicKey, sessionNamespace, true)
                clearAllDialogs()
            },
            {
                // reject session
                // todo: localize reason?
                var reason = "User rejection"
                if (!networksSupported) {
                    reason = "Network(s) not supported"
                } else if (!methodOrEventSupported) {
                    reason = "Method(s) or Event(s) not supported"
                } else if (!extensionsSupported) {
                    reason = "Extension(s) not supported"
                }
                viewModel.rejectSessionV2(meta.proposerPublicKey, reason, true)
                clearAllDialogs()
            }
        ).apply {
            // todo: and enable/disable connect button?
            var networkNames = WalletConnectRepositoryImpl.proposalNamespacesToChainNames(eip155ProposalNamespace)
            when {
                numberOfNonEip155Chains == 1 -> networkNames = networkNames + "Non EVM Chain" // todo: localize
                numberOfNonEip155Chains > 1 -> networkNames = networkNames + "Non EVM Chains" // todo: localize
            }
            setView(
                meta,
                ViewDetailsV2(
                    networkNames,
                    getString(R.string.connect_to_website),
                    getString(R.string.connect)
                )
            )
            var walletConnectV2AlertType = WalletConnectV2AlertType.NO_ALERT
            if (!networksSupported) {
                walletConnectV2AlertType = WalletConnectV2AlertType.UNSUPPORTED_NETWORK_WARNING
            } else if (!methodOrEventSupported) {
                walletConnectV2AlertType = WalletConnectV2AlertType.OTHER_UNSUPPORTED
            }
            setWarnings(walletConnectV2AlertType)

            // todo: set addresses in spinner instead of accounts
            //updateAccountSpinner()
            show()
        }
    }

    // todo: for walletconnect 2.0 set a list of addresses instead of accounts
    private fun DappConfirmationDialogV1.updateAccountSpinner() {
        setupAccountSpinner(viewModel.account.id, viewModel.availableAccounts) { account ->
            viewModel.setNewAccount(account)
            //change state of confirm button for prevent the same db records
            changeClickableConfirmButtonState(account.address, account.chainId)
        }
    }

    // todo: set warnings etc. for walletconnect 2.0 as well.
    private fun DappConfirmationDialogV1.handleNetwork(
        network: BaseNetworkData,
        dialogType: WalletConnectAlertType,
        meta: WalletConnectPeerMeta = WalletConnectPeerMeta())
    {
        when (dialogType) {
            WalletConnectAlertType.NO_ALERT -> setNoAlert()
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING, WalletConnectAlertType.CHANGE_ACCOUNT -> {
                setNotDefinedNetworkWarning(viewModel.availableNetworks, dialogType, meta.chainId) { chainId ->
                    viewModel.setAccountForSelectedNetwork(chainId)
                    updateAccountSpinner()
                }
                if (WalletConnectAlertType.CHANGE_ACCOUNT == dialogType) {
                    //set default network for equal state in "networkAdapter" and "accountAdapter"
                    if (Int.InvalidId == meta.chainId) {
                        viewModel.setAccountForSelectedNetwork(viewModel.availableNetworks.first().chainId )
                    } else {
                        //set current DApp session network like current account(s) network (for show it like chosen in spinner)
                        viewModel.setAccountForSelectedNetwork(meta.chainId)
                        //set current DApp session address(account) like current account (for show it like chosen in spinner)
                        viewModel.availableAccounts
                            .find { meta.address == it.address }
                            ?.let { viewModel.setNewAccount(it) }
                    }
                }
            }
            WalletConnectAlertType.CHANGE_ACCOUNT_WARNING -> setChangeAccountMessage(network.name)
            WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR -> setNoAvailableAccountMessage(network)
            WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING -> setUnsupportedNetworkMessage(network)
        }
    }

    private fun updateConnectionDialog(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog?.apply {
            if (this is DappConfirmationDialogV1) {
                updateAccountSpinner()
                handleNetwork(network, dialogType)
            }
        }
    }

    private fun handleWCConnectionDeepLink(deepLink: String) {
        viewModel.handleDeepLink(deepLink)
    }

    private fun clearAllDialogs() {
        dappDialog = null
        errorDialog = null
        loadingDialog = null
        confirmationDialogDialog = null
    }

    private fun dismissDialogs() {
        loadingDialog?.dismiss()
        errorDialog?.dismiss()
    }

    private fun closeDialog() {
        clearAllDialogs()
    }

    private fun closeToBackground() {
        clearAllDialogs()
        //go back to browser after connect established(from "Services" page (DApp list))
        moveTaskToBack(true)
    }

    companion object {
        const val MOBILE_WALLET_CONNECT_DATA = "mobile_wallet_connect_data"
    }
}