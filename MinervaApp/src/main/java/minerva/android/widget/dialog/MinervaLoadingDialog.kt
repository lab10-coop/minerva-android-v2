package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import minerva.android.databinding.MinervaLoadingDialogBinding

class MinervaLoadingDialog(context: Context) : Dialog(context) {

    private val binding: MinervaLoadingDialogBinding = MinervaLoadingDialogBinding.inflate(layoutInflater)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
    }

}