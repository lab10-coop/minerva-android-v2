package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dapp_confirmation_dialog.*
import minerva.android.R
import minerva.android.databinding.DappConfirmationDialogBinding
import minerva.android.walletConnect.model.session.WCPeerMeta

class DappConfirmationDialog(context: Context, connect: () -> Unit, deny: () -> Unit) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    private val binding: DappConfirmationDialogBinding =
        DappConfirmationDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        setCancelable(false)
        with(binding) {
            confirmationView.hideRequestedData()
            cancel.setOnClickListener {
                deny()
                dismiss()
            }
            this@DappConfirmationDialog.connect.setOnClickListener {
                connect()
                dismiss()
            }
        }
        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                deny()
                dismiss()
            }
            true
        }
    }

    fun setView(meta: WCPeerMeta) = with(binding) {
        confirmationView.setIcon(meta.icons[0])
        name.text = meta.name
        Glide.with(context)
            .load(meta.icons[0])
            .into(icon)
    }

    fun setNetworkName(name: String) {
        network.text = name
    }
}