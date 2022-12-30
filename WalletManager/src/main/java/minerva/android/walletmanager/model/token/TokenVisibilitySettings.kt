package minerva.android.walletmanager.model.token

import java.util.*

class TokenVisibilitySettings(private val map: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()) {

    fun updateTokenVisibility(accountAddress: String, tokenAddress: String, visibility: Boolean): TokenVisibilitySettings {
        accountAddress.lowercase(Locale.ROOT).let { accountAddress ->
            tokenAddress.lowercase(Locale.ROOT).let { tokenAddress ->
                (map[accountAddress] ?: mutableMapOf()).apply {
                    this[tokenAddress] = visibility
                    map[accountAddress] = this
                }
            }
        }
        return this
    }

    fun getTokenVisibility(accountAddress: String, tokenAddress: String): Boolean? =
        map[accountAddress.lowercase(Locale.ROOT)]?.get(tokenAddress.lowercase(Locale.ROOT))
}