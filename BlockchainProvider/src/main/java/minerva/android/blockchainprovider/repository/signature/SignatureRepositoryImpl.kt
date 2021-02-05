package minerva.android.blockchainprovider.repository.signature

import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import timber.log.Timber
import wallet.core.jni.*

class SignatureRepositoryImpl : SignatureRepository {

    override fun signData(data: String, privateKey: String, mnemonic: String): String {

        Timber.tag("kobe").d("data $data")

        val messageBytes = if (data.startsWith("0x")) {
            SignatureJava.hexStringToByteArray(data)
        } else {
            data.toByteArray()
        }

        Timber.tag("kobe").d("messageBytes $messageBytes")
        System.loadLibrary("TrustWalletCore")

        //spr privKey.sign = NOPE
        //spr credentials.create() i sign.sign message = NOPE
        //Hash.keccak256(ethMessage) z load wallet trust = NOPE
        //master privKey i credentials = NOPE
        //spr privKey in alpha =NOPE
        //przepchnij mnemonic i uzyj HDWALLET = nope
        //Java = sprawdz - NOPE

        //dane wejsciowe, obczaj jakie sa privKeys =
        //kolejnosc wysylania argumentów, spr w jakie eventy wpada w minervie i w alpha =


//        val credentials = Credentials.create(privateKey) //ECKeyPair.create(Numeric.hexStringToByteArray(privateKey))
        //        val signetData = Sign.signMessage(toSign, credentials.ecKeyPair)
//        val signedBytes = bytesFromSignature(signetData)

        val ethMessage = SignatureJava.getEthereumMessage(messageBytes)

        Timber.tag("kobe").d("ethMessage $ethMessage")


        val newWallet = HDWallet(mnemonic, "")
        val pk: PrivateKey = newWallet.getKeyForCoin(CoinType.ETHEREUM)

        val toSign = Hash.keccak256(ethMessage) //sha3(ethMessage)//Hash.keccak256(ethMessage);

        Timber.tag("kobe").d("toSign $toSign")

//        val credentials =  Credentials.create(privateKey)//pk.sign(toSign, Curve.SECP256K1)//PrivateKey().sign(toSign, Curve.SECP256K1)

//       val signedBytes = Sign.signMessage(toSign, credentials.ecKeyPair)

//        Timber.tag("kobe").d("signedBytes $signedBytes")

//        val toSend = SignatureJava.bytesFromSignature(signedBytes)
        val toSend = pk.sign(toSign, Curve.SECP256K1)
        Timber.tag("kobe").d("toSend $toSend")


        val vcComponent = SignatureJava.patchSignatureVComponent(toSend)

        Timber.tag("kobe").d("vcComponent $vcComponent")

        Timber.tag("kobe").d("Numeric.toHexString(vcComponent) ${Numeric.toHexString(vcComponent)}")

        return Numeric.toHexString(vcComponent)
    }
}