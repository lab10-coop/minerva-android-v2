package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import minerva.android.databinding.ConfirmationButtonsBinding

class DappDialogButtons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ConfirmationButtonsBinding =
        ConfirmationButtonsBinding.inflate(LayoutInflater.from(context), this, true)

    fun setView(approve: () -> Unit, deny: () -> Unit, dismiss: () -> Unit) {
        with(binding) {
            cancel.setOnClickListener {
                deny()
                dismiss()
            }
            connect.setOnClickListener {
                approve()
                dismiss()
            }

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    deny()
                    dismiss()
                }
                true
            }
        }
    }
}