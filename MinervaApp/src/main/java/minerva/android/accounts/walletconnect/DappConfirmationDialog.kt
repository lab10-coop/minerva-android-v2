package minerva.android.accounts.walletconnect

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dapp_confirmation_dialog.*
import minerva.android.R
import minerva.android.databinding.DappConfirmationDialogBinding

class DappConfirmationDialog(context: Context, connect: () -> Unit) :
    BottomSheetDialog(context, R.style.CustomBottomSheetDialog) {

    private val binding: DappConfirmationDialogBinding = DappConfirmationDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        setCancelable(false)
        with(binding) {
            confirmationView.apply {
                hideRequestedData()
                setConnectionIcon(R.drawable.ic_artis_sigma)
            }
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
}