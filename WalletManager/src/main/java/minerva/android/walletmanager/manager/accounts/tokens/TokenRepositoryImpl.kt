package minerva.android.walletmanager.manager.accounts.tokens

import androidx.annotation.VisibleForTesting

class TokenRepositoryImpl : TokenRepository {

    @VisibleForTesting
    val iconUrls: Map<String, String> by lazy {
        getTokensIconURLs()
    }

    override fun getIconURL(chainId: Int, address: String): String? = iconUrls[prepareKey(chainId, address)]

    private fun getTokensIconURLs(): Map<String, String> = mapOf()

    private fun prepareKey(chainId: Int, address: String) = "$chainId$address"

}