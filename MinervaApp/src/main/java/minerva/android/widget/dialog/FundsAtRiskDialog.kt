package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import minerva.android.R
import minerva.android.databinding.FundsAtRiskDialogLayoutBinding

class FundsAtRiskDialog(context: Context) : Dialog(context, R.style.DialogStyle) {

    private val binding = FundsAtRiskDialogLayoutBinding.inflate(LayoutInflater.from(context))

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        initView()
    }

    private fun initView() {
        binding.confirmButton.setOnClickListener {
            dismiss()
        }
    }
}