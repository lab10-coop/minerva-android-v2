package minerva.android.utils

import android.util.Log

/**
 * Minerva Logger - object for work with log
 */
object MinervaLogger {
    val tag = "my_log"
    val pref = "___"

    fun l(data: List<Any?>? = mutableListOf()) {
        data?.forEach { Log.e(tag, "$pref$it") }
    }

    fun l(data: Any? = "") = Log.e(tag, "$pref$data")

    fun lp() = Log.e(tag, "_____________________")
}