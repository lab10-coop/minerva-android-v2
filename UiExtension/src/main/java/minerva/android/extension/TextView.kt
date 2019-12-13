package minerva.android.extension

import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

fun TextView.setDrawableRight(@DrawableRes drawableResource: Int) {
    this.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawableResource, 0)
}

fun TextView.textRes(@StringRes res: Int) {
    text = this.resources.getString(res)
}