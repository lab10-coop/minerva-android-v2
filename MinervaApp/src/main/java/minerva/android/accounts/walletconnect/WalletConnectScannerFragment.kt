package minerva.android.accounts.walletconnect

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.login.scanner.BaseScannerFragment
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setConnectionStatusFlowable()
        observeViewState()
        showWalletConnectViews()
        setupBottomSheet()
        setupRecycler()
    }

    private fun observeViewState() {
        viewModel.stateLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is WrongQrCodeState -> handleWrongQrCode()
                is CorrectQrCodeState -> shouldScan = false
                is OnDisconnected -> showToast(getString(R.string.dapp_disconnected))
                is ProgressBarState -> {
                    if (!it.show) {
                        binding.scannerProgressBar.invisible()
                    }
                }
                is OnSessionRequestWithDefinedNetwork ->
                    showConnectionDialog(it.meta, it.network, true)
                is OnSessionRequestWithUndefinedNetwork ->
                    showConnectionDialog(it.meta, it.network, false)
                is UpdateDappsState -> dappsAdapter.updateDapps(it.dapps)
                is HideDappsState -> {
                    with(binding) {
                        dappsBottomSheet.dapps.gone()
                        closeButton.margin(bottom = DEFAULT_MARGIN)
                    }
                }
                is OnSessionDeleted -> showToast(getString(R.string.dapp_deleted))
                is OnError -> {
                    handleError(it.error)
                }
            }
        })
        viewModel.errorLiveData.observe(viewLifecycleOwner, EventObserver {
            handleError(it)
        })
    }

    private fun handleError(error: Throwable) {
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
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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

    private fun showConnectionDialog(meta: WalletConnectPeerMeta, network: String, isNetworkDefined: Boolean) {
        DappConfirmationDialog(requireContext(),
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
            handleNetwork(isNetworkDefined)
            show()
        }
    }

    private fun DappConfirmationDialog.handleNetwork(isNetworkDefined: Boolean) {
        when {
            !isNetworkDefined -> setNotDefinedNetworkWarning()
            isNetworkDefined && viewModel.shouldChangeNetwork ->
                setWrongNetworkMessage(getString(R.string.wrong_network_message, viewModel.requestedNetwork))
        }
    }

    override fun onCloseButtonAction() {
        viewModel.closeScanner()
    }

    override fun onPermissionNotGranted() {
        viewModel.closeScanner()
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