package minerva.android.kotlinUtils.function

inline fun <R> R?.orElse(block: () -> R): R {
    return this ?: block()
}