package minerva.android.walletmanager.model.token

import java.util.*

class TokenVisibilitySettings(private val map: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()) {

    fun updateTokenVisibility(networkAddress: String, assetAddress: String, visibility: Boolean): TokenVisibilitySettings {
        networkAddress.toLowerCase(Locale.ROOT).let { networkAddress ->
            assetAddress.toLowerCase(Locale.ROOT).let { assetAddress ->
                (map[networkAddress] ?: mutableMapOf()).apply {
                    this[assetAddress] = visibility
                    map[networkAddress] = this
                }
            }
        }
        return this
    }

    fun getTokenVisibility(networkAddress: String, tokenAddress: String): Boolean? =
        map[networkAddress.toLowerCase(Locale.ROOT)]?.get(tokenAddress.toLowerCase(Locale.ROOT))
}