package minerva.android.accounts.walletconnect

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScannerFragment
import minerva.android.walletmanager.exception.InvalidAccountException
import minerva.android.walletmanager.model.WalletConnectPeerMeta
import minerva.android.widget.DappConfirmationDialog
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
                is OnError -> {
                    shouldScan = true
                    showToast(getErrorMessage(it))
                }
                is OnDisconnected ->
                    showToast(getString(R.string.dapp_disconnected))
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
            }
        })
    }

    private fun getErrorMessage(it: OnError) =
        if (it.error is InvalidAccountException) {
            getString(R.string.invalid_account_message)
        } else {
            it.error.message
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

    override fun setupCallbacks() {
        codeScanner.apply {
            decodeCallback = DecodeCallback { result ->
                requireActivity().runOnUiThread {
                    if (shouldScan) {
                        binding.scannerProgressBar.visible()
                        viewModel.handleQrCode(result.text)
                    }
                }
            }

            errorCallback = ErrorCallback { handleCameraError(it) }
        }
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
            setView(meta)
            setNetworkName(network)
            handleNetwork(isNetworkDefined)
            show()
        }
    }

    private fun DappConfirmationDialog.handleNetwork(isNetworkDefined: Boolean) {
        when {
            !isNetworkDefined && !viewModel.shouldChangeNetwork -> setNotDefinedNetwork()
            !isNetworkDefined && viewModel.shouldChangeNetwork -> setNotDefinedNetworkWarning()
            isNetworkDefined && viewModel.shouldChangeNetwork -> setWrongNetworkMessage(
                getString(
                    R.string.wrong_network_message,
                    viewModel.requestedNetwork
                )
            )
        }
    }

    override fun setOnCloseButtonListener() {
        binding.closeButton.setOnClickListener { viewModel.closeScanner() }
    }

    override fun onPermissionNotGranted() {
        viewModel.closeScanner()
    }

    companion object {
        const val PEEK_HEIGHT = 240
        const val DEFAULT_MARGIN = 32f
        const val INCREASED_MARGIN = 115f
        const val FIRST_ICON = 0
    }
}