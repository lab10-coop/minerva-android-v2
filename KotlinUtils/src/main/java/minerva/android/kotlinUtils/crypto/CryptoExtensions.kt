package minerva.android.kotlinUtils.crypto

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

const val HEX_PREFIX = "0x"

fun ByteArray.toHexString(): String = toHexString(this, 0, this.size, false)

fun String.toByteArray(): ByteArray = hexStringToByteArray(this)

val String.getFormattedMessage: String
    get() = if (this.isJSONValid) {
        JSONObject(this).toString(3)
    } else {
        this
    }

private val String.isJSONValid: Boolean
    get() {
        try {
            JSONObject(this)
        } catch (ex: JSONException) {
            try {
                JSONArray(this)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

val String.hexToUtf8: String
    get() {
        var hex = this
        hex = cleanHexPrefix(hex)
        val buff = ByteBuffer.allocate(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            buff.put(hex.substring(i, i + 2).toInt(16).toByte())
            i += 2
        }
        buff.rewind()
        val cb = StandardCharsets.UTF_8.decode(buff)
        return cb.toString()
    }

private fun toHexString(input: ByteArray, offset: Int, length: Int, withPrefix: Boolean): String {
    val stringBuilder = StringBuilder()
    if (withPrefix) {
        stringBuilder.append("0x")
    }
    for (i in offset until offset + length) {
        stringBuilder.append(String.format("%02x", input[i] and 0xFF))
    }
    return stringBuilder.toString()
}

private infix fun Byte.and(mask: Int): Int = toInt() and mask

fun hexStringToByteArray(input: String): ByteArray {
    val cleanInput: String = cleanHexPrefix(input)
    val len = cleanInput.length
    if (len == 0) {
        return byteArrayOf()
    }
    val data: ByteArray
    val startIdx: Int
    if (len % 2 != 0) {
        data = ByteArray(len / 2 + 1)
        data[0] = Character.digit(cleanInput[0], 16).toByte()
        startIdx = 1
    } else {
        data = ByteArray(len / 2)
        startIdx = 0
    }
    var i = startIdx
    while (i < len) {
        data[(i + 1) / 2] = ((Character.digit(cleanInput[i], 16) shl 4)
                + Character.digit(cleanInput[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

private fun cleanHexPrefix(input: String): String =
    if (containsHexPrefix(input)) {
        input.substring(2)
    } else {
        input
    }

private fun containsHexPrefix(input: String): Boolean =
    input.length > 1 && input[0] == '0' && input[1] == 'x'