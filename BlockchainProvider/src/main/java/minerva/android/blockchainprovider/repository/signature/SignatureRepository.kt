package minerva.android.blockchainprovider.repository.signature

interface SignatureRepository {
    fun signData(data: String, privateKey: String): String
}