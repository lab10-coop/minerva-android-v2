package minerva.android.walletmanager.manager.accounts.tokens

interface TokenRepository {
    fun getIconURL(chainId: Int, address: String): String?
}