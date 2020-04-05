package minerva.android.cryptographyProvider.repository

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import me.uport.sdk.core.padBase64
import me.uport.sdk.core.toBase64
import me.uport.sdk.jwt.InvalidJWTException
import me.uport.sdk.jwt.JWTTools
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
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toNoPrefixHexString
import timber.log.Timber
import java.security.SecureRandom
import java.util.*


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

    override fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>> {
        val keysSubject: SingleSubject<Map<String, Any?>> = SingleSubject.create()
        return try {
            val payload = JWTTools().decodeRaw(jwtToken).second
            handleTokenExpired(payload)
            keysSubject.apply { onSuccess(payload) }
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
            keysSubject.apply { onError(Throwable()) }
        }
    }

    override suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String =
        JWTTools().createJWT(payload, getDIDKey(privateKey), KPSigner(privateKey))

    private fun getDIDKey(key: String) = "did:ethr:${KPSigner(key).getAddress()}"

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

    private fun handleTokenExpired(payload: Map<String, Any?>) {
        if (isTokenValidYet(payload)) {
            throw InvalidJWTException("Jwt not valid yet (issued in the future) iat: ${payload["iat"]}")
        }
        if (isTokenExpired(payload)) {
            throw InvalidJWTException("JWT has expired: exp: ${payload["exp"]}")
        }
    }

    private fun isTokenValidYet(payload: Map<String, Any?>) =
        payload[IAT] != null && payload[IAT] as Long > (System.currentTimeMillis() / 1000 + TIME_SKEW)

    private fun isTokenExpired(payload: Map<String, Any?>) =
        payload[EXP] != null && payload[EXP] as Long <= (System.currentTimeMillis() / 1000 - TIME_SKEW)

    companion object {
        private const val TIME_SKEW = 300L
        private const val IAT = "iat"
        private const val EXP = "exp"
        private const val DERIVED_PATH_PREFIX = "m/99'/"
        private const val MASTER_KEYS_DERIVED_PATH = "m/"
        private const val ENTROPY_SIZE = 128 / 8
        private const val RADIX = 16
    }
}