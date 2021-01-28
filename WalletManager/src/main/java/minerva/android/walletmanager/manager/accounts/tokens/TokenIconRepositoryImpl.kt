package minerva.android.walletmanager.manager.accounts.tokens

import minerva.android.walletmanager.BuildConfig
import java.net.URL

class TokenIconRepositoryImpl : TokenIconRepository {
    override fun getIconRawFile(): String = URL(BuildConfig.ERC20_TOKEN_DATA_URL).readText()
}