package minerva.android.walletmanager.model.token

import java.util.*

class TokenVisibilitySettings(private val map: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()) {

    fun updateTokenVisibility(accountAddress: String, tokenAddress: String, visibility: Boolean): TokenVisibilitySettings {
        accountAddress.lowercase(Locale.ROOT).let { accountAddressLower ->
            tokenAddress.lowercase(Locale.ROOT).let { tokenAddressLower ->
                (map[accountAddressLower] ?: mutableMapOf()).apply {
                    this[tokenAddressLower] = visibility
                    map[accountAddressLower] = this
                }
            }
        }
        return this
    }

    fun getTokenVisibility(accountAddress: String, tokenAddress: String): Boolean? =
        map[accountAddress.lowercase(Locale.ROOT)]?.get(tokenAddress.lowercase(Locale.ROOT))
}