package minerva.android.blockchainprovider.repository.validation

import org.web3j.crypto.WalletUtils
import java.util.*


class ValidationRepositoryImpl(
    private val checksumRepository: ChecksumRepository
) : ValidationRepository {

    override fun isAddressValid(address: String, chainId: Int?): Boolean {
        return when (chainId) {
            RSK_MAIN, RSK_TEST -> WalletUtils.isValidAddress(address) &&
                    (checksumRepository.toEIP1191Checksum(address, chainId) == address ||
                            isAddressWithoutChecksum(address))
            else -> isAddressValid(address)
        }
    }

    private fun isAddressValid(address: String): Boolean =
        WalletUtils.isValidAddress(address) &&
                (checksumRepository.toEIP55Checksum(address) == address ||
                        isAddressWithoutChecksum(address))

    private fun isAddressWithoutChecksum(address: String) =
        address.toLowerCase(Locale.ROOT) == address

    override fun toChecksumAddress(address: String, chainId: Int?): String {
        return when (chainId) {
            RSK_MAIN, RSK_TEST -> checksumRepository.toEIP1191Checksum(address, chainId)
            else -> checksumRepository.toEIP55Checksum(address)
        }
    }

    override fun toRecipientChecksum(address: String, chainId: Int?): String {
        return when (chainId) {
            RSK_MAIN, RSK_TEST -> address.toLowerCase(Locale.ROOT)
            else -> address
        }
    }

    private val RSK_MAIN = 30
    private val RSK_TEST = 31
}