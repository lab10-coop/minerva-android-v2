package minerva.android.extension

import android.widget.TextView

fun TextView.setTextWithArgs(stringRes: Int, vararg arguments: Any) {
    text = context.getString(stringRes, *arguments)
}
