package minerva.android.utils


fun ensureHexColorPrefix(hexColor: String): String {
    return when {
        hexColor.startsWith(HEX_PREFIX) -> hexColor
        else -> HEX_PREFIX + hexColor
    }
}

private const val HEX_PREFIX = "#"


