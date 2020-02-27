package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.finger_print_view_layout.view.*
import minerva.android.R
import minerva.android.payment.listener.FingerPrintListener

class FingerPrintView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    private lateinit var listener: FingerPrintListener

    fun setListener(listener: FingerPrintListener) {
        this.listener = listener
    }

    init {
        inflate(context, R.layout.finger_print_view_layout, this)
    }

    fun setOnClickListeners() {
        setOnCancelButtonOnClickListener()
        setOnFingerPrintOnClickListener()
    }

    private fun setOnFingerPrintOnClickListener() {
        fingerPrintIcon.setOnClickListener {
            listener.onFingerPrintClicked()
        }
    }

    private fun setOnCancelButtonOnClickListener() {
        cancelButton.setOnClickListener {
            listener.onCancelClicked()
        }
    }
}