package minerva.android.walletmanager.utils

private const val SLASH = "/"
private const val ENCODED_SLASH = "%2F"

object PublicKeyUtils {
    fun encodePublicKey(publicKey: String) = publicKey.replace(
        SLASH,
        ENCODED_SLASH
    )
}