package minerva.android.extension

import android.util.Patterns.EMAIL_ADDRESS

fun String.isEmail() : Boolean {
    return EMAIL_ADDRESS.matcher(this).matches()
}