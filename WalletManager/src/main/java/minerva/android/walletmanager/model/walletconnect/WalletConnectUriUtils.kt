package minerva.android.walletmanager.model.walletconnect

object WalletConnectUriUtils {
    private val walletConnectPattern = Regex("^wc:([a-zA-Z0-9-]+)(@[1-9][0-9]*)?(\\?.*)?$")

    fun walletConnectVersionFromUri(uri: String): Int? {
        return walletConnectPattern.find(uri)?.groups?.get(2)?.value?.substring(1)?.toInt()
    }

    fun isValidWalletConnectUri(uri: String): Boolean {
        return isValidWalletConnectUriV1(uri) || isValidWalletConnectUriV2(uri)
    }

    fun isValidWalletConnectUriV1(uri: String): Boolean {
        if (!walletConnectPattern.matches(uri)) {
            return false
        }
        if (walletConnectVersionFromUri(uri) != 1) {
            return false
        }
        val requiredParams = Regex("(bridge|key)=[^&]+")
        return requiredParams.containsMatchIn(uri)
    }

    fun isValidWalletConnectUriV2(uri: String): Boolean {
        if (!walletConnectPattern.matches(uri)) {
            return false
        }
        if (walletConnectVersionFromUri(uri) != 2) {
            return false
        }
        val requiredParams = Regex("(symKey|relay-protocol)=[^&]+")
        return requiredParams.containsMatchIn(uri)
    }
}