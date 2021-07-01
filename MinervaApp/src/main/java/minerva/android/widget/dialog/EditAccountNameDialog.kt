package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.EditNameDialogBinding
import minerva.android.extension.afterTextChanged
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class EditAccountNameDialog(context: Context, private val account: Account, private val onConfirmClick: (Account, String) -> Unit) :
    Dialog(context, R.style.DialogStyle) {

    private val binding = EditNameDialogBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(true)
        setupListeners()
        setupView()
    }

    private fun setupListeners() = with(binding) {
        cancelButton.setOnClickListener { dismiss() }
        confirmButton.setOnClickListener {
            onConfirmClick(account, accountNameEdit.text.toString())
            dismiss()
        }
        accountNameEdit.afterTextChanged { confirmButton.isEnabled = it.isNotBlank() }
    }

    private fun setupView() = with(binding) {
        val name = getAccountNameWithoutIndex(account.name)
        accountNameEdit.setText(name)
        accountNameEdit.setSelection(name.length)
    }

    private fun getAccountNameWithoutIndex(accountName: String): String =
        accountName.split(SPACE).toMutableList().apply {
            removeAt(FIRST_ELEMENT_INDEX)
        }.joinToString(SPACE)


    companion object {
        private const val SPACE = " "
        private const val FIRST_ELEMENT_INDEX = 0
    }

}