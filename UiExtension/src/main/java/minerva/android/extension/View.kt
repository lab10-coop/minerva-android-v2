package minerva.android.extension

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager

fun View.visible() {
    visibility = View.VISIBLE
    isEnabled = true
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    isEnabled = false
    visibility = View.GONE
}

fun View.visibleOrGone(isVisible: Boolean) {
    if (isVisible) visible()
    else gone()
}

fun View.visibleOrInvisible(isVisible: Boolean) {
    if (isVisible) visible()
    else invisible()
}

fun View.toggleVisibleOrGone() {
    visibility = if (visibility != View.VISIBLE) View.VISIBLE
    else View.GONE
}

fun View.margin(left: Float? = null, top: Float? = null, right: Float? = null, bottom: Float? = null) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.run { leftMargin = dpToPx(this) }
        top?.run { topMargin = dpToPx(this) }
        right?.run { rightMargin = dpToPx(this) }
        bottom?.run { bottomMargin = dpToPx(this) }
    }
}

inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
private fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

fun View.hideKeyboard() = (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
    .hideSoftInputFromWindow(windowToken, 0)

fun View.showKeyboard() = (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
    .toggleSoftInput(InputMethodManager.SHOW_FORCED,0)

fun View.addOnGlobalLayoutListener(onPrepare: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            onPrepare()
            this@addOnGlobalLayoutListener.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}
