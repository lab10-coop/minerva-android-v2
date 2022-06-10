package minerva.android.extension

import android.util.Patterns.EMAIL_ADDRESS
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

fun String.isEmail(): Boolean = EMAIL_ADDRESS.matcher(this).matches() || isEmpty()
val String.Companion.empty: String
    get() = ""

fun String?.isNumber() = if (this.isNullOrEmpty()) false else this.all { Character.isDigit(it) }

/**
* From Json Array To List - create list from specified json array
* @return List<T> - json array
*/
fun <T> String.fromJsonArrayToList(): List<T> {
    val type: Type = object : TypeToken<List<T>>() {}.type
    return Gson().fromJson(this, type)
}

/**
 * To Json Array - create string with json array from specified string
 * @return string - json array
 */
fun String.toJsonArray(): String = Gson().toJson(listOf(this))