package minerva.android.cryptographyProvider.repository

import android.util.Log
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import me.uport.sdk.core.hexToByteArray
import me.uport.sdk.core.padBase64
import me.uport.sdk.core.toBase64
import me.uport.sdk.ethrdid.EthrDIDNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.InvalidJWTException
import me.uport.sdk.jwt.JWTEncodingException
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.JwtPayload
import me.uport.sdk.signer.KPSigner
import me.uport.sdk.signer.getUncompressedPublicKeyWithPrefix
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import org.kethereum.bip39.entropyToMnemonic
import org.kethereum.bip39.mnemonicToEntropy
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toKey
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
import org.kethereum.crypto.toAddress
import org.kethereum.model.ECKeyPair
import org.komputing.khex.extensions.toNoPrefixHexString
import timber.log.Timber
import java.security.SecureRandom
import java.util.*

/*Derivation path for identities and values "m/99'/n" where n is index of identity and value*/
class CryptographyRepositoryImpl : CryptographyRepository {

    override fun createMasterSeed(): Single<Triple<String, String, String>> {
        ByteArray(ENTROPY_SIZE).run {
            SecureRandom().nextBytes(this)
            getMasterKeys(toNoPrefixHexString()).run { return Single.just(Triple(toNoPrefixHexString(), getPublicKey(), getPrivateKey())) }
        }
    }

    private fun getMasterKeys(seed: String): ECKeyPair =
        MnemonicWords(getMnemonicForMasterSeed(seed)).toKey(MASTER_KEYS_DERIVED_PATH).keyPair

    override fun getMnemonicForMasterSeed(seed: String): String =
        entropyToMnemonic(seed.hexToByteArray(), WORDLIST_ENGLISH)

    override fun computeDeliveredKeys(seed: String, index: Int): Single<DerivedKeys> {
        MnemonicWords(getMnemonicForMasterSeed(seed)).toKey(getDerivedPath(index)).keyPair.run {
            return Single.just(DerivedKeys(index, getPublicKey(), getPrivateKey(), getAddress()))
        }
    }

    override fun restoreMasterSeed(mnemonic: String): Single<Triple<String, String, String>> {
        return try {
            val seed = mnemonicToEntropy(mnemonic, WORDLIST_ENGLISH).toNoPrefixHexString()
            getMasterKeys(seed).run { Single.just(Triple(seed, getPublicKey(), getPrivateKey())) }
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
            Single.error(exception)
        }
    }

    private fun ECKeyPair.getPublicKey(): String = getUncompressedPublicKeyWithPrefix().toBase64().padBase64()

    private fun ECKeyPair.getPrivateKey(): String = privateKey.key.toString(RADIX)

    private fun ECKeyPair.getAddress(): String = toAddress().hex

    private fun getDerivedPath(index: Int) = "${DERIVED_PATH_PREFIX}$index"

    override fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>> =
        rxSingle {
            try {
                val payload: JwtPayload? = JWTTools().verify(jwtToken, getResolver())

                /*JWTTools().decodeRaw(jwtToken).second is used for decoding payload from jwtToken, because JwtPayload, which is automatically
                * generated from JWTTools().verify(jwtToken, resolver) is missing essential fields, hence this object is omitted*/



                if (payload != null) {
                    Log.e("klop", "payload is not null")
                    JWTTools().decodeRaw(jwtToken).second
                } else {
                    Log.e("klop", "Payload is NULL")
                    error(Throwable("JWT Payload is null"))
                }
            } catch (exception: InvalidJWTException) {
                error(InvalidJWTException("Invalid JWT Exception: ${exception.message}"))
            } catch (exception: JWTEncodingException) {
                error(JWTEncodingException("JWT Encoding Exception: ${exception.message}"))
            }
        }

    private fun getResolver(): EthrDIDResolver =
        EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork(NETWORK_ID, REGISTRY_ADDRESS, JsonRPC(JSON_RPC_URL)))
            .build()

    override fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String> =
        rxSingle { JWTTools().createJWT(payload, getDIDKey(privateKey), KPSigner(privateKey)) }

    private fun getDIDKey(key: String) = "$DID_PREFIX${KPSigner(key).getAddress()}"

    override fun validateMnemonic(mnemonic: String): List<String> {
        mutableListOf<String>().apply {
            collectInvalidWords(StringTokenizer(mnemonic), this)
            return if (this.isEmpty()) emptyList() else this
        }
    }

    private fun collectInvalidWords(phase: StringTokenizer, list: MutableList<String>) {
        while (phase.hasMoreTokens()) {
            val word = phase.nextToken()
            if (!WORDLIST_ENGLISH.contains(word)) {
                list.add(word)
            }
        }
    }

    companion object {
        private const val DERIVED_PATH_PREFIX = "m/99'/"
        private const val MASTER_KEYS_DERIVED_PATH = "m/"
        private const val ENTROPY_SIZE = 128 / 8
        private const val RADIX = 16
        private const val DID_PREFIX = "did:ethr:"

        /*Those parameters for EthrDIDResolver should be taken based on NetworkID, hence all registry addresses are now stored on Artis Tau1,
         for now it is okay to keep them hardcoded. In the future those data should be generated dynamically based
         on what kind of network given registry is stored*/
        private const val NETWORK_ID = "artis_t1"
        private const val REGISTRY_ADDRESS = "0xdCa7EF03e98e0DC2B855bE647C39ABe984fcF21B"
        private const val JSON_RPC_URL = "https://rpc.tau1.artis.network"
    }
}