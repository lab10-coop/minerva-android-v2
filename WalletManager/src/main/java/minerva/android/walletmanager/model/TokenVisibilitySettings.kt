package minerva.android.walletmanager.model

import java.util.*

class TokenVisibilitySettings(private val map: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()) {

    fun updateAssetVisibility(networkAddress: String, assetAddress: String, visibility: Boolean): TokenVisibilitySettings {
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

    fun getAssetVisibility(networkAddress: String, assetAddress: String): Boolean? =
        map[networkAddress.toLowerCase(Locale.ROOT)]?.get(assetAddress.toLowerCase(Locale.ROOT))
}