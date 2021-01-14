package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dapp_confirmation_dialog.*
import minerva.android.R
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.walletConnect.model.session.WCPeerMeta

class DappConfirmationDialog(context: Context, connect: () -> Unit) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    private val binding: DappConfirmationDialogBinding =
        DappConfirmationDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        setCancelable(false)
        with(binding) {
            confirmationView.hideRequestedData()
            cancel.setOnClickListener {
                dismiss()
            }
            this@DappConfirmationDialog.connect.setOnClickListener {
                connect()
            }
        }
        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) dismiss()
            true
        }
    }

    fun setView(meta: WCPeerMeta) {
        binding.confirmationView.setIcon(meta.icons[0])
    }
}