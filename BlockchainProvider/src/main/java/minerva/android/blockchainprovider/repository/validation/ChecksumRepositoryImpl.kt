package minerva.android.blockchainprovider.repository.validation

import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.lang.StringBuilder
import java.util.*

class ChecksumRepositoryImpl : ChecksumRepository {
    override fun toEIP55Checksum(address: String): String = Keys.toChecksumAddress(address)

    override fun toEIP1191Checksum(address: String, chainId: Int): String {
        val lowercaseAddress = Numeric.cleanHexPrefix(address).lowercase(Locale.ROOT)
        val hashInput = chainId.toString() + HEX_PREFIX + lowercaseAddress
        val addressHash = Numeric.cleanHexPrefix(Hash.sha3String(hashInput))

        return StringBuilder().apply {
            ensureCapacity(lowercaseAddress.length + HEX_PREFIX.length)
            append(HEX_PREFIX)
            lowercaseAddress.indices.forEach { i ->
                if (addressHash[i].toString().toInt(RADIX) >= THRESHOLD) {
                    append(lowercaseAddress[i].toString().uppercase(Locale.ROOT))
                } else {
                    append(lowercaseAddress[i])
                }
            }
        }.toString()
    }

    companion object {
        private const val HEX_PREFIX = "0x"
        private const val RADIX = 16
        private const val THRESHOLD = 8
    }
}