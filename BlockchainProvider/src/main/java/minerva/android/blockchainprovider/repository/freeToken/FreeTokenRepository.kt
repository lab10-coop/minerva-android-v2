package minerva.android.blockchainprovider.repository.freeToken

interface FreeTokenRepository {
    fun getFreeATS(address: String): String
}