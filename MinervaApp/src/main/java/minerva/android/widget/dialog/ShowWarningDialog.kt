package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import minerva.android.R
import minerva.android.databinding.ShowWarningDialogAccountBinding

/**
 * Show Warning Dialog - dialog window which show that chosen item is on "Unmaintained Network" status
 * @param context - instance of android.content.Context
 * @param needCallback - if it's TRUE include "showWarning" switcher and callback
 * @param onOkButtonClick - callback which will be called when "okButton" presses
 */
class ShowWarningDialog(context: Context,
                        private val needCallback: Boolean,
                        private val onOkButtonClick: (state: Boolean) -> Unit = {}) :
    Dialog(context, R.style.DialogStyle) {

    private val binding = ShowWarningDialogAccountBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setupListeners()
        binding.apply {
            showWarningContainer.visibility = if (needCallback) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() = with(binding) {
        okButton.setOnClickListener {
            //passing desirable state for "ShowWarningDialog" (for hide/show in future)
            if (needCallback) onOkButtonClick(showWarningState.isChecked)

            dismiss()
        }
    }
}