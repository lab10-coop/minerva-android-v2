package minerva.android.extension

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding3.widget.textChangeEvents
import io.reactivex.Observable
import io.reactivex.Observable.merge
import io.reactivex.android.schedulers.AndroidSchedulers
import minerva.android.extension.validator.ValidationResult
import java.util.concurrent.TimeUnit

private const val RIGHT_DRAWABLE_INDEX = 2
private const val VISIBLE = 255
private const val INVISIBLE = 0
private const val NO_IMAGE = 0
private const val EMPTY_STRING = ""
private const val TEXT_WATCHER_DEBOUNCE = 500L
private const val FIRST = 1L
private const val NO_ICON = 0

fun EditText.getValidationObservable(
    inputLayout: TextInputLayout? = null,
    checkFunction: (String) -> ValidationResult
): Observable<Boolean> =
    textChangeEvents()
        .publish { observable ->
            merge(
                observable.take(FIRST).filter { it.text.isNotBlank() },
                observable.skip(FIRST)
            )
        }
        .debounce(TEXT_WATCHER_DEBOUNCE, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .map { checkFunction(it.text.toString()) }
        .doOnNext {
            inputLayout?.apply {
                error = if (it.hasError) {
                    setErrorIconDrawable(NO_ICON)
                    context.getString(it.errorMessageId)
                } else {
                    isErrorEnabled = false
                    null
                }
            }
        }
        .map { it.isSuccessful }

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun EditText.onFocusLost(onFocusLost: (String) -> Unit) =
    this.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) onFocusLost(this.text.toString())
    }

fun EditText.clearButton() {
    setCompoundDrawablesWithIntrinsicBounds(NO_IMAGE, NO_IMAGE, R.drawable.ic_clear, NO_IMAGE)
    text?.let {
        drawableRightVisibility(it.isEmpty())
    }
    afterTextChanged {
        drawableRightVisibility(it.isNotEmpty())
    }
    onRightDrawableClicked {
        it.setText(EMPTY_STRING)
    }
}

fun EditText.drawableRightVisibility(isVisible: Boolean) {
    compoundDrawables[RIGHT_DRAWABLE_INDEX]?.let {
        it.alpha = if(isVisible) VISIBLE
        else INVISIBLE
    }
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.onRightDrawableClicked(onClicked: (view: EditText) -> Unit) {
    this.setOnTouchListener { view, event ->
        var hasConsumed = false
        if (view is EditText) {
            if (event.x >= view.width - view.totalPaddingRight) {
                if (event.action == MotionEvent.ACTION_UP) {
                    onClicked(this)
                }
                hasConsumed = true
            }
        }
        hasConsumed
    }
}