package minerva.android.widget.dialog.walletconnect

import android.content.Context
import android.view.KeyEvent
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import minerva.android.R
import minerva.android.databinding.DappDialogButtonsBinding
import minerva.android.databinding.DappNetworkHeaderBinding

abstract class DappDialog(context: Context, val approve: () -> Unit = {}, val deny: () -> Unit = {}) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    abstract val networkHeader: DappNetworkHeaderBinding

    init {
        this.setCancelable(false)
    }

    fun initButtons(confirmationButtons: DappDialogButtonsBinding) {
        with(confirmationButtons) {
            cancel.setOnClickListener {
                deny()
                dismiss()
            }
            connect.setOnClickListener {
                approve()
                dismiss()
            }

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    deny()
                    dismiss()
                }
                true
            }
        }
    }

    fun setupHeader(dapppName: String, networkName: String, icon: Any) = with(networkHeader) {
        name.text = dapppName
        network.text = networkName
        Glide.with(context)
            .load(icon)
            .into(networkHeader.icon)
    }
}