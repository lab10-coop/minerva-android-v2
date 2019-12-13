package minerva.android.extension

import android.content.Context
import android.view.KeyEvent
import android.view.View
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

fun View.setVisible(isVisible: Boolean) {
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
