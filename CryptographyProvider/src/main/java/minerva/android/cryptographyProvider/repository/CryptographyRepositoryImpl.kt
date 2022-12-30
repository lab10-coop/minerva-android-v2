package minerva.android.cryptographyProvider.repository

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
import me.uport.sdk.signer.KPSigner
import me.uport.sdk.signer.UNCOMPRESSED_PUBLIC_KEY_SIZE
import me.uport.sdk.signer.normalize
import minerva.android.cryptographyProvider.repository.model.DerivationPath.Companion.MASTER_KEYS_PATH
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.cryptographyProvider.repository.model.Seed
import minerva.android.cryptographyProvider.repository.model.SeedError
import minerva.android.cryptographyProvider.repository.model.SeedWithKeys
import minerva.android.cryptographyProvider.repository.throwable.InvalidJwtThrowable
import org.kethereum.bip39.entropyToMnemonic
import org.kethereum.bip39.mnemonicToEntropy
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toKey
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
import org.kethereum.crypto.toAddress
import org.kethereum.extensions.toBytesPadded
import org.kethereum.model.ECKeyPair
import org.komputing.khex.extensions.toNoPrefixHexString
import org.spongycastle.math.ec.custom.djb.Curve25519
import timber.log.Timber
import java.security.SecureRandom
import java.util.*

class CryptographyRepositoryImpl(private val jwtTools: JWTTools) : CryptographyRepository {

    override fun createMasterSeed(): Single<Triple<String, String, String>> {
        with(ByteArray(ENTROPY_SIZE)) {
            SecureRandom().nextBytes(this)
            val seed = toNoPrefixHexString()
            val mnemonic = entropyToMnemonic(seed.hexToByteArray(), WORDLIST_ENGLISH)
            MnemonicWords(mnemonic).toKey(MASTER_KEYS_PATH, "").keyPair.apply {
                return Single.just(Triple(seed, getPublicKey(), getPrivateKey()))
            }
        }
    }

    override fun getMnemonicForMasterSeed(seed: String): String =
        entropyToMnemonic(seed.hexToByteArray(), WORDLIST_ENGLISH)

    override fun calculateDerivedKeysSingle(
        seed: String,
        password: String,
        index: Int,
        derivationPathPrefix: String,
        isTestNet: Boolean
    ): Single<DerivedKeys> = Single.just(calculateDerivedKeys(seed, password, index, derivationPathPrefix, isTestNet))

    override fun calculateDerivedKeys(
        seed: String,
        password: String,
        index: Int,
        derivationPathPrefix: String,
        isTestNet: Boolean
    ): DerivedKeys {
        val derivationPath = "${derivationPathPrefix}$index"
        val keys = MnemonicWords(getMnemonicForMasterSeed(seed)).toKey(derivationPath, password).keyPair
        return DerivedKeys(index, keys.getPublicKey(), keys.getPrivateKey(), keys.getAddress(), isTestNet)
    }

    override fun restoreMasterSeed(mnemonicAndPassword: String): Seed =
        try {
            var password = ""
            var mnemonic = MnemonicWords(mnemonicAndPassword)
            // if not a multiple of 3, try to use last word as password.
            if (mnemonic.words.size % 3 > 0) {
                password = mnemonic.words.last()
                mnemonic = MnemonicWords(mnemonicAndPassword.substringBeforeLast(" "))
            }
            val seed: String = mnemonic.mnemonicToEntropy(WORDLIST_ENGLISH).toNoPrefixHexString()
            val keys: ECKeyPair = mnemonic.toKey(MASTER_KEYS_PATH, password).keyPair
            SeedWithKeys(seed, password, keys.getPublicKey(), keys.getPrivateKey())
        } catch (exception: Exception) {
            Timber.e(exception)
            SeedError(exception)
        }

    /**
     * Decompresses the public key of this pair and returns the uncompressed version, including prefix
     */
    private fun ECKeyPair.getUncompressedPublicKeyWithPrefix(): ByteArray {
        val pubBytes = this.publicKey.normalize().key.toBytesPadded(UNCOMPRESSED_PUBLIC_KEY_SIZE)
        pubBytes[0] = 0x04
        return pubBytes
    }

    private fun ECKeyPair.getPublicKey(): String = getUncompressedPublicKeyWithPrefix().toBase64().padBase64()

    private fun ECKeyPair.getPrivateKey(): String = String.format(PRIVATE_KEY_FORMAT, privateKey.key)

    private fun ECKeyPair.getAddress(): String = toAddress().hex

    override fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>> =
        rxSingle {
            try {
                /*JWTTools().decodeRaw(jwtToken).second is used for decoding payload from jwtToken, because JwtPayload, which is automatically
                * generated from JWTTools().verify(jwtToken, resolver) is missing essential fields, hence this object is omitted*/
                jwtTools.verify(jwtToken, getResolver())
                jwtTools.decodeRaw(jwtToken).second
            } catch (exception: InvalidJWTException) {
                throw InvalidJwtThrowable("Invalid JWT Exception: ${exception.message}")
            } catch (exception: JWTEncodingException) {
                throw InvalidJwtThrowable("JWT Encoding Exception: ${exception.message}")
            } catch (exception: IllegalArgumentException) {
                throw InvalidJwtThrowable("Illegal argument exception: ${exception.message}")
            }
        }

    private fun getResolver(): EthrDIDResolver =
        EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork(NETWORK_ID, REGISTRY_ADDRESS, JsonRPC(JSON_RPC_URL)))
            .build()

    override fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String> =
        rxSingle { jwtTools.createJWT(payload, getDIDKey(privateKey), KPSigner(privateKey)) }

    private fun getDIDKey(key: String) = "$DID_PREFIX${KPSigner(key).getAddress()}"

    override fun areMnemonicWordsValid(mnemonicAndPassword: String): Boolean =
        mutableListOf<String>().apply { collectInvalidWords(StringTokenizer(mnemonicAndPassword), this) }.isEmpty()

    private fun collectInvalidWords(phrase: StringTokenizer, list: MutableList<String>) {
        val multipleOfThree = phrase.countTokens() % 3 <= 0
        while (phrase.hasMoreTokens()) {
            val word = phrase.nextToken()
            // Don't check the last word against the wordlist as it might be a password
            if (!(!multipleOfThree && !phrase.hasMoreTokens())) {
                if (!WORDLIST_ENGLISH.contains(word)) {
                    list.add(word)
                }
            }
        }
    }

    companion object {
        private const val ENTROPY_SIZE = 128 / 8
        private const val DID_PREFIX = "did:ethr:"
        private const val PRIVATE_KEY_FORMAT = "%064x"

        /*Those parameters for EthrDIDResolver should be taken based on NetworkID, hence all registry addresses are now stored on Artis Tau1,
         for now it is okay to keep them hardcoded. In the future those data should be generated dynamically based
         on what kind of network given registry is stored*/
        private const val NETWORK_ID = "artis_t1"
        private const val REGISTRY_ADDRESS = "0xdCa7EF03e98e0DC2B855bE647C39ABe984fcF21B"
        private const val JSON_RPC_URL = "https://rpc.tau1.artis.network"
    }
}