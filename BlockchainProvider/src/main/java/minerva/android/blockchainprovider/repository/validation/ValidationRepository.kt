package minerva.android.blockchainprovider.repository.validation

interface ValidationRepository {
    fun isAddressValid(address: String, chainId: Int? = null): Boolean
    fun toChecksumAddress(address: String, chainId: Int? = null): String
    fun toRecipientChecksum(address: String, chainId: Int? = null): String
}