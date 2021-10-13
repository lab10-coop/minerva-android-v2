package minerva.android.blockchainprovider.repository.validation

interface ChecksumRepository {
    fun toEIP55Checksum(address: String): String
    fun toEIP1191Checksum(address: String, chainId: Int): String
}