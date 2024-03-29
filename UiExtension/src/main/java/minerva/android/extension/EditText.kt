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

private const val TEXT_WATCHER_DEBOUNCE = 1000L
private const val FIRST = 1L
const val NO_ICON = 0

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

fun EditText.onTextChanged(onTextChanged: (String, Int, Int) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged.invoke(text.toString(), start, count)
        }

        override fun afterTextChanged(editable: Editable?) {
        }
    })
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