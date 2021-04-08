package minerva.android.extension

import android.util.TypedValue
import android.view.ViewGroup

fun ViewGroup.addRippleEffect() {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
    setBackgroundResource(outValue.resourceId)
}