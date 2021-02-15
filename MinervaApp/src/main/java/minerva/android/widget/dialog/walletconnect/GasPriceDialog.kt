package minerva.android.widget.dialog.walletconnect

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import minerva.android.R
import minerva.android.databinding.GasPriceDialogBinding

class GasPriceDialog(context: Context) : Dialog(context, R.style.MaterialDialogStyle) {

    private val binding: GasPriceDialogBinding = GasPriceDialogBinding.inflate(layoutInflater)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
        with(binding) {
            buttons.cancel.setOnClickListener {
                dismiss()
            }

            buttons.connect.setOnClickListener {
                //todo get value from edit text
                dismiss()
            }
        }

    }

}