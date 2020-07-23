package minerva.android.extension.wrapper

import android.text.Editable
import android.text.TextWatcher

abstract class TextWatcherWrapper : TextWatcher {

    abstract fun onTextChanged(s: CharSequence?)

    override fun afterTextChanged(s: Editable?) {
        // do nothing
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // do nothing
    }

    override fun onTextChanged(content: CharSequence?, start: Int, before: Int, count: Int) {
        onTextChanged(content)
    }

}