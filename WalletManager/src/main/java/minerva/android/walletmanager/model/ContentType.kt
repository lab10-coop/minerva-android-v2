package minerva.android.walletmanager.model

enum class ContentType {
    VIDEO,
    IMAGE,
    ENCODED_IMAGE,
    INVALID;
}

inline fun <reified T : Enum<T>> valueOf(type: String): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}