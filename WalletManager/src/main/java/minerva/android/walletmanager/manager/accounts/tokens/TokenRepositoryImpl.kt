package minerva.android.walletmanager.manager.accounts.tokens

import minerva.android.kotlinUtils.Empty

class TokenRepositoryImpl : TokenRepository {

    private val iconUrls: Map<String, String> by lazy {
        getTokensIconURLs()
    }

    override fun getIconURL(chainId: Int, address: String): String = iconUrls[prepareKey(chainId, address)] ?: String.Empty

    private fun getTokensIconURLs(): Map<String, String> = mapOf()

    private fun prepareKey(chainId: Int, address: String) = "$chainId$address"

}