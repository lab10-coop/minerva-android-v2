package minerva.android.walletmanager.model

enum class ContentType {
    VIDEO,
    IMAGE,
    ENCODED_IMAGE,
    INVALID;
}

/**
 * Address Status - status of account address which use for correct displaying
 */
enum class AddressStatus {
    FREE,
    HIDDEN,
    ALREADY_IN_USE;
}

inline fun <reified T : Enum<T>> valueOf(type: String): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}