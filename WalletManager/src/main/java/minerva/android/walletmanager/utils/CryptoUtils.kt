package minerva.android.walletmanager.utils

import minerva.android.walletmanager.model.Network

private const val SLASH = "/"
private const val ENCODED_SLASH = "%2F"

object CryptoUtils {
    fun encodePublicKey(publicKey: String) = publicKey.replace(
        SLASH,
        ENCODED_SLASH
    )

    fun prepareName(network: Network, position: Int) = String.format(VALUE_NAME_PATTERN, position, network.full)
    private const val VALUE_NAME_PATTERN = "#%d %s"
}