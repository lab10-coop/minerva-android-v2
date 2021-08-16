package minerva.android.walletConnect

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
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.MinervaLoadingDialog
import minerva.android.widget.dialog.walletconnect.*
import org.koin.android.ext.android.inject
import java.math.BigDecimal

abstract class BaseWalletConnectInteractionsActivity : AppCompatActivity() {

    private val walletConnectViewModel: WalletConnectInteractionsViewModel by inject()

    private var dappDialog: DappDialog? = null
    private var loadingDialog: MinervaLoadingDialog? = null
    private var confirmationDialogDialog: DappConfirmationDialog? = null
    private var errorDialog: AlertDialog? = null

    abstract fun isProtectTransactionEnabled(): Boolean

    private fun rejectRequest() {
        dappDialog?.let {
            it.dismiss()
            walletConnectViewModel.rejectRequest()
        }
    }

    override fun onPause() {
        super.onPause()
        rejectRequest()
        walletConnectViewModel.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        walletConnectViewModel.dispose()
        clearAllDialogs()
    }

    protected fun prepareWalletConnect() {
        prepareWalletConnectInteractionObservers()
        walletConnectViewModel.getWalletConnectSessions()
        intent?.getParcelableExtra<Uri>(MOBILE_WALLET_CONNECT_DATA)?.let {
            handleWCConnectionDeepLink(it.toString())
        }
    }

    private fun prepareWalletConnectInteractionObservers() {
        walletConnectViewModel.walletConnectStatus.observe(this@BaseWalletConnectInteractionsActivity, Observer { state ->
            dismissDialogs()
            when (state) {
                is OnEthSignRequest -> dappDialog = getDappSignDialog(state)
                is OnEthSendTransactionRequest -> dappDialog = getSendTransactionDialog(state)
                is ProgressBarState -> handleLoadingDialog(state)
                is OnGeneralError -> handleWalletConnectGeneralError(state.error.message)
                is OnWalletConnectTransactionError -> handleWalletConnectTransactionError(state)
                is WrongTransactionValueState -> handleWrongTransactionValueState(state)
                is OnDisconnected -> handleWalletConnectDisconnectState(state.sessionName)
                is OnWalletConnectConnectionError -> handleWalletConnectError(state.sessionName)
                is OnSessionRequest -> showConnectionDialog(state.meta, state.network, state.dialogType)
                is UpdateOnSessionRequest -> updateConnectionDialog(state.network, state.dialogType)
                is CloseScannerState -> closeToBackground()
                is CorrectWalletConnectCodeState -> handleLoadingDialogForWCConnection(true)
                else -> {
                }
            }
        })
        walletConnectViewModel.errorLiveData.observe(this, EventObserver { handleWalletConnectGeneralError(it.message) })
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
        walletConnectViewModel.logToFirebase("Transaction with invalid value: ${state.transaction}, firebaseId: $firebaseID")
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
        with(walletConnectViewModel) {
            DappSendTransactionDialog(
                this@BaseWalletConnectInteractionsActivity,
                {
                    if (isProtectTransactionEnabled()) getCurrentFragment()?.showBiometricPrompt(
                        ::sendTransaction,
                        ::rejectRequest
                    )
                    else sendTransaction()
                },
                {
                    rejectRequest()
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
                walletConnectViewModel.recalculateTxCost(BigDecimal(gasPrice), it.transaction),
                it.account
            ) { balance, cost -> walletConnectViewModel.isBalanceTooLow(balance, cost) }
        }.show()
    }

    private fun getDappSignDialog(it: OnEthSignRequest) =
        DappSignMessageDialog(this@BaseWalletConnectInteractionsActivity,
            { walletConnectViewModel.acceptRequest() },
            { walletConnectViewModel.rejectRequest() }
        ).apply {
            setContent(it.message, it.session)
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
        if (sessionName.isNotEmpty()) {
            showFlashbar(String.empty, getString(R.string.dapp_disconnected, sessionName))
        } else {
            showAlertDialog(getString(R.string.session_connection_error))
        }
    }

    private fun showConnectionDialog(meta: WalletConnectPeerMeta, network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog = DappConfirmationDialog(this,
            {
                walletConnectViewModel.approveSession(meta)
                clearAllDialogs()
            },
            {
                walletConnectViewModel.rejectSession()
                clearAllDialogs()
            },
            { chainId ->
                walletConnectViewModel.addAccount(chainId, dialogType)
            }).apply {
            setView(meta, network.name)
            handleNetwork(network, dialogType)
            updateAccountSpinner()
            show()
        }
    }

    private fun DappConfirmationDialog.updateAccountSpinner() {
        setupAccountSpinner(walletConnectViewModel.account.id, walletConnectViewModel.availableAccounts) { account ->
            walletConnectViewModel.setNewAccount(account)
        }
    }

    private fun DappConfirmationDialog.handleNetwork(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        when (dialogType) {
            WalletConnectAlertType.NO_ALERT -> setNoAlert()
            WalletConnectAlertType.UNDEFINED_NETWORK_WARNING -> setNotDefinedNetworkWarning(walletConnectViewModel.availableNetworks) { chainId ->
                walletConnectViewModel.setAccountForSelectedNetwork(chainId)
                updateAccountSpinner()
            }
            WalletConnectAlertType.CHANGE_ACCOUNT_WARNING -> setChangeAccountMessage(network.name)
            WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR -> setNoAvailableAccountMessage(network)
            WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING -> setUnsupportedNetworkMessage(network.chainId.toString())
        }
    }

    private fun updateConnectionDialog(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        confirmationDialogDialog?.apply {
            updateAccountSpinner()
            handleNetwork(network, dialogType)
        }
    }

    private fun handleWCConnectionDeepLink(deepLink: String) {
        walletConnectViewModel.subscribeToWCConnectionStatusFlowable()
        walletConnectViewModel.handleDeepLink(deepLink)
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

    private fun closeToBackground() {
        clearAllDialogs()
        moveTaskToBack(true)
    }

    companion object {
        const val MOBILE_WALLET_CONNECT_DATA = "mobile_wallet_connect_data"
    }
}