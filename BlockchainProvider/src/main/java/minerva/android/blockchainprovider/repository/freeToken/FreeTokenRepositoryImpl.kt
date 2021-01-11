package minerva.android.blockchainprovider.repository.freeToken

import minerva.android.blockchainprovider.BuildConfig
import java.net.URL

class FreeTokenRepositoryImpl : FreeTokenRepository {
    override fun getFreeATS(address: String): String = URL(BuildConfig.FREE_ATS_URL + address).readText()
}