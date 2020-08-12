package minerva.android.kotlinUtils.extension

private const val MILLISECONDS_LIMIT = 9999999999L
private const val MILLISECONDS_MULTIPLIER = 1000

fun Long.toMilliseconds() =
    if (this < MILLISECONDS_LIMIT) this * MILLISECONDS_MULTIPLIER
    else this