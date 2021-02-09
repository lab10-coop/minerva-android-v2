package minerva.android.blockchainprovider.repository.signature

import minerva.android.blockchainprovider.repository.web3j.StructuredDataEncoder
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.crypto.hexStringToByteArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

class SignatureRepositoryImpl : SignatureRepository {

    override fun signData(data: String, privateKey: String): String {
        val messageBytes = if (data.startsWith(HEX_PREFIX)) {
            hexStringToByteArray(data)
        } else {
            data.toByteArray()
        }
        val signature = Sign.signPrefixedMessage(messageBytes, Credentials.create(privateKey).ecKeyPair)
        val toSend = signature.run { r + s + v }
        return Numeric.toHexString(toSend)
    }

    override fun signTypedData(data: String, privateKey: String): String {
        val signature =
            Sign.signMessage(StructuredDataEncoder(data).structuredData, Credentials.create(privateKey).ecKeyPair)
        val toSend = signature.run { r + s + v }
        return Numeric.toHexString(toSend)
    }
}