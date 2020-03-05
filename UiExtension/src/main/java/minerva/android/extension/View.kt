package minerva.android.extension

import android.content.Context
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visibleOrGone(isVisible: Boolean) {
    if (isVisible) visible()
    else gone()
}

const val FADE_DURATION = 250L
fun View.fadeOut(endAction: () -> Unit = {}) {
    animate().alpha(0f).setDuration(FADE_DURATION).withEndAction {
        gone()
        endAction()
    }
}

fun View.fadeIn(endAction: () -> Unit = {}) {
    visible()
    animate().alpha(1f).setDuration(FADE_DURATION).withEndAction { endAction() }
}

fun View.fadeOutToInvisible(endAction: () -> Unit = {}) {
    animate().alpha(0f).setDuration(FADE_DURATION).withEndAction {
        invisible()
        endAction()
    }
}

fun View.showKeyboard() {
    getInputMethodManager().toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
}

fun View.hideKeyboard() {
    getInputMethodManager().hideSoftInputFromWindow(this.windowToken, 0)
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
fun Context.dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

private fun View.getInputMethodManager() =
    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

fun View.enterKeyboardAction(keyAction: () -> Unit = {}) {
    this.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.action == KeyEvent.ACTION_UP) {
                keyAction()
                return@OnKeyListener true
            } else {
                return@OnKeyListener false
            }
        }
        false
    })
}
