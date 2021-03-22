package minerva.android.walletmanager.model.token

import java.util.*

class TokenVisibilitySettings(private val map: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()) {

    fun updateTokenVisibility(accountAddress: String, tokenAddress: String, visibility: Boolean): TokenVisibilitySettings {
        accountAddress.toLowerCase(Locale.ROOT).let { accountAddress ->
            tokenAddress.toLowerCase(Locale.ROOT).let { tokenAddress ->
                (map[accountAddress] ?: mutableMapOf()).apply {
                    this[tokenAddress] = visibility
                    map[accountAddress] = this
                }
            }
        }
        return this
    }

    fun getTokenVisibility(accountAddress: String, tokenAddress: String): Boolean? =
        map[accountAddress.toLowerCase(Locale.ROOT)]?.get(tokenAddress.toLowerCase(Locale.ROOT))
}