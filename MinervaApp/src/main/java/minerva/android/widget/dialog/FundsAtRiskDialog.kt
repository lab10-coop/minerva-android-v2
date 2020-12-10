package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import kotlinx.android.synthetic.main.funds_at_risk_dialog_layout.*
import minerva.android.R

class FundsAtRiskDialog(context: Context) : Dialog(context, R.style.DialogStyle) {

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.funds_at_risk_dialog_layout)
        setCancelable(false)
        initView()
    }

    private fun initView() {
        confirmButton.setOnClickListener {
            dismiss()
        }
    }
}