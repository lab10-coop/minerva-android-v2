package minerva.android.accounts.walletconnect

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.extension.visibleOrInvisible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class WalletConnectScannerFragment : BaseWalletConnectScannerFragment() {

    override val viewModel: WalletConnectViewModel by sharedViewModel()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private val dappsAdapter: DappsAdapter by lazy {
        DappsAdapter { peerId -> viewModel.killSession(peerId) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.subscribeToWCConnectionStatusFlowable()
        observeViewState()
        showWalletConnectViews()
        setupBottomSheet()
        setupRecycler()
    }

    private fun observeViewState() {
        viewModel.stateLiveData.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is WrongWalletConnectCodeState -> handleWrongQrCode()
                is CorrectWalletConnectCodeState -> shouldScan = false
                is OnDisconnected -> handleWalletConnectDisconnectState(state.sessionName)
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