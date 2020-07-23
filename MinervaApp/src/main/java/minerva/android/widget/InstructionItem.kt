package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.instruction_item.view.*
import minerva.android.R

class InstructionItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.instruction_item, this, true)
    }

    fun setIcon(drawable: Int) {
        instructionIcon.setImageResource(drawable)
    }

    fun setContent(content: String) {
        instructionContent.text = content
    }

    fun setTitle(title: String) {
        instructionTitle.text = title
    }
}