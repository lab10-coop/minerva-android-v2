package minerva.android.accounts.walletconnect

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import minerva.android.extension.gone
import minerva.android.extension.margin
import minerva.android.extension.visible
import minerva.android.services.login.scanner.BaseScannerFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

open class WalletConnectScannerFragment : BaseScannerFragment() {

    val viewModel: WalletConnectViewModel by sharedViewModel()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private val dappsAdapter: DappsAdapter by lazy {
        DappsAdapter(viewModel.dapps) { Toast.makeText(context, "Available soon...", Toast.LENGTH_LONG).show() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showWalletConnectViews()
        setupBottomSheet()
        setupRecycler()
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
                    if (shouldScan) {
                        //TODO handle wallet connect qr
                        context?.let { showDialog(it) }
                        shouldScan = false
                    }
                }
            }

            errorCallback = ErrorCallback { handleCameraError(it) }
        }
    }

    private fun showDialog(it: Context) {
        with(DappConfirmationDialog(it) { Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show() }) {
            setOnDismissListener { shouldScan = true }
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