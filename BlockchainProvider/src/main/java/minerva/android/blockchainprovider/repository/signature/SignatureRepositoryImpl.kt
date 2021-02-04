package minerva.android.blockchainprovider.repository.signature

import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.util.*

class SignatureRepositoryImpl : SignatureRepository {

    override fun signData(data: String, privateKey: String): String {

        val messageBytes = if (data.startsWith("0x")) {
            Numeric.hexStringToByteArray(data)
        } else {
            data.toByteArray()
        }


        val ethMessage = getEthereumMessage(messageBytes)
        val credentials = Credentials.create(privateKey)//ECKeyPair.create(Numeric.hexStringToByteArray(privateKey))
        val signetData = Sign.signMessage(ethMessage, credentials.ecKeyPair)
        val signedBytes = bytesFromSignature(signetData)//signetData.run { r + s + v }
//        val vcComponent = patchSignatureVComponent(signedBytes)
        return Numeric.toHexString(signedBytes)
    }

    private fun patchSignatureVComponent(signature: ByteArray?): ByteArray? {
        if (signature != null && signature.size == 65 && signature[64] < 27) {
            signature[64] = (signature[64] + 0x1b.toByte()).toByte()
        }
        return signature
    }


    private fun getEthereumMessage(message: ByteArray): ByteArray {
        val prefix: ByteArray = "\u0019Ethereum Signed Message:\n + ${message.size}".toByteArray()
        val result = ByteArray(prefix.size + message.size)
        System.arraycopy(prefix, 0, result, 0, prefix.size)
        System.arraycopy(message, 0, result, prefix.size, message.size)
        return result
    }

    private fun bytesFromSignature(signature: Sign.SignatureData): ByteArray {
        val sigBytes = ByteArray(65)
        Arrays.fill(sigBytes, 0.toByte())
        try {
            System.arraycopy(signature.r, 0, sigBytes, 0, 32)
            System.arraycopy(signature.s, 0, sigBytes, 32, 32)
            System.arraycopy(signature.v, 0, sigBytes, 64, 1)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        return sigBytes
    }
}