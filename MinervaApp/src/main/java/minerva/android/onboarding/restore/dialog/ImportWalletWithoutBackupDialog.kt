package minerva.android.onboarding.restore.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.ImportWalletWithoutBackupDialogBinding

class ImportWalletWithoutBackupDialog(context: Context, private val onImport: () -> Unit) :
    Dialog(context, R.style.DialogStyle) {

    private val binding = ImportWalletWithoutBackupDialogBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(true)
        setupListeners()
        binding.warningMessage.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupListeners() = with(binding) {
        cancelButton.setOnClickListener { dismiss() }
        importButton.setOnClickListener {
            onImport()
            dismiss()
        }
    }
}