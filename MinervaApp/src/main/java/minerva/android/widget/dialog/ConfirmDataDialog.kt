package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import kotlinx.android.synthetic.main.confirm_data_dialog_layout.*
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.widget.HorizontalLabeledTextView

class ConfirmDataDialog(
    context: Context,
    private var titleText: String?,
    private var data: Map<String, String>?,
    private var positiveAction: () -> Unit,
    private var negativeAction: () -> Unit
) : Dialog(context, R.style.ConfirmDialog) {

    private fun initializeView() {
        title.text = titleText ?: String.Empty
        prepareData()
        cancelButton.setOnClickListener { onNegativeAction() }
        send_button.setOnClickListener { onPositiveAction() }
    }

    private fun prepareData() =
        data?.let {
            it.forEach { data ->
                HorizontalLabeledTextView(context).apply {
                    setTitleAndBody(data.key, data.value)
                    container.addView(this)
                }
            }
        }


    private fun onPositiveAction() {
        positiveAction()
        dismiss()
    }

    private fun onNegativeAction() {
        negativeAction()
        dismiss()
    }

    data class Builder(private val context: Context) {
        private var title: String = String.Empty
        private var data: Map<String, String> = mapOf()
        private var positiveAction: () -> Unit = {}
        private var negativeAction: () -> Unit = {}

        fun title(title: String) = apply { this.title = title }
        fun data(data: Map<String, String>) = apply { this.data = data }
        fun positiveAction(positiveAction: () -> Unit) = apply { this.positiveAction = positiveAction }
        fun negativeAction(negativeAction: () -> Unit) = apply { this.negativeAction = negativeAction }
        fun show() = ConfirmDataDialog(context, title, data, positiveAction, negativeAction).show()
    }

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.confirm_data_dialog_layout)
        initializeView()
    }
}