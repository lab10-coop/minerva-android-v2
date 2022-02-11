package minerva.android.widget.dialog.walletconnect

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import minerva.android.R
import minerva.android.databinding.GasLimitAndPriceDialogBinding
import minerva.android.extension.showKeyboard

class GasLimitAndPriceDialog(
    context: Context,
    initialGasPrice: String,
    initialGasLimit: String,
    approve: (gasPrice: String, gasLimit: String) -> Unit
) : Dialog(context, R.style.MaterialDialogStyle) {

    private val binding = GasLimitAndPriceDialogBinding.inflate(layoutInflater)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        with(binding) {
            setContentView(root)
            buttons.cancel.setOnClickListener {
                dismiss()
            }

            buttons.confirm.setOnClickListener {
                approve(price.text.toString(), limit.text.toString())
                dismiss()
            }
            price.setText(initialGasPrice)
            limit.setText(initialGasLimit)
        }
    }

    fun focusOnAmountAndShowKeyboard() {
        with(binding.price) {
            requestFocus()
            showKeyboard()
        }
    }
}