package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.DialogHideUnmaintainedAccountBinding
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class HideAccountUnmaintainedDialog(context: Context, private val account: Account, private val onHideClick: (Account) -> Unit) :
    Dialog(context, R.style.DialogStyle) {

    private val binding = DialogHideUnmaintainedAccountBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(true)
        setupListeners()
    }

    private fun setupListeners() = with(binding) {
        cancelButton.setOnClickListener { dismiss() }
        hideButton.setOnClickListener {
            onHideClick(account)
            dismiss()
        }
    }
}