package minerva.android.utils

import android.util.Log

/**
 * !!!never mind, just local helper!!!
 */
object MyHelper {
    val tag = "my_log"
    val pref = "___"
    fun l(data: List<Any?>? = mutableListOf()) {
        data?.forEach { Log.e(tag, "$pref$it") }
    }
    fun l(data: Any? = "") {
        Log.e(tag, "$pref$data")
    }
}