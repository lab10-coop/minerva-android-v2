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
import minerva.android.walletConnect.model.session.WCPeerMeta
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class WalletConnectScannerFragment : BaseScannerFragment() {

    val viewModel: WalletConnectViewModel by sharedViewModel()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private val dappsAdapter: DappsAdapter by lazy {
        DappsAdapter(viewModel.dapps) {
            viewModel.killSession()
            //todo delete from list
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewState()
        showWalletConnectViews()
        setupBottomSheet()
        setupRecycler()
    }

    private fun observeViewState() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer {
            binding.scannerProgressBar.invisible()
            when (it) {
                is WrongQrCodeState -> handleWrongQrCode()
                is CorrectQrCodeState -> shouldScan = false
                is OnError -> Toast.makeText(context, it.error.message, Toast.LENGTH_SHORT).show()
                is OnWCSessionRequest -> showConnectionDialog(it.meta)
                is OnWCDisconnected -> Toast.makeText(context, it.reason, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleWrongQrCode() {
        Toast.makeText(context, getString(R.string.scan_wc_qr), Toast.LENGTH_SHORT).show()
        shouldScan = true
    }

    private fun setupBottomSheet() = with(binding) {
        with(BottomSheetBehavior.from(dappsBottomSheet.dapps)) {
            bottomSheetBehavior = this
            peekHeight = PEEK_HEIGHT
        }

        val bottomMargin = if (viewModel.dapps.isEmpty()) {
            dappsBottomSheet.dapps.gone()
            DEFAULT_MARGIN
        } else {
            INCREASED_MARGIN
        }
        closeButton.margin(bottom = bottomMargin)
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
                    binding.scannerProgressBar.visible()
                    if (shouldScan) {
                        viewModel.handleQrCode(result.text)
                    }
                }
            }

            errorCallback = ErrorCallback { handleCameraError(it) }
        }
    }

    private fun showConnectionDialog(meta: WCPeerMeta) {
        DappConfirmationDialog(requireContext(),
            {
                viewModel.approveSession()
                viewModel.dapps.add(Dapp(name = meta.name, icon = meta.icons[0]))
                dappsAdapter.updateDapps(viewModel.dapps)
                binding.dappsBottomSheet.dapps.visible()
                shouldScan = true
                binding.scannerProgressBar.invisible()
            },
            {
                viewModel.rejectSession()
                shouldScan = true
                binding.scannerProgressBar.invisible()
            }).apply {
            setOnDismissListener { shouldScan = true }
            setView(meta, viewModel.networkName)
            show()
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
    }
}