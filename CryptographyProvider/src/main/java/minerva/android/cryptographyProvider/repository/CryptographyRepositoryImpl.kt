package minerva.android.cryptographyProvider.repository

import android.content.Context
import com.uport.sdk.signer.UportHDSigner
import com.uport.sdk.signer.encryption.KeyProtection
import io.reactivex.Single
import me.uport.sdk.jwt.InvalidJWTException
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.signer.KPSigner
import minerva.android.kotlinUtils.Empty
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
import timber.log.Timber
import java.util.*


class CryptographyRepositoryImpl(var contex: Context) : CryptographyRepository {

    override fun createMasterKey(callback: (error: Exception?, rootAddress: String, rootPublicKey: String) -> Unit) {
        UportHDSigner().createHDSeed(contex, KeyProtection.Level.SIMPLE, callback)
    }

    override fun showMnemonicForMasterKey(privateKey: String, prompt: String, callback: (error: Exception?, seedPhase: String) -> Unit) {
        UportHDSigner().showHDSeed(contex, privateKey, prompt, callback)
    }

    override fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        UportHDSigner().importHDSeed(contex, KeyProtection.Level.SIMPLE, mnemonic, callback)
    }

    override fun computeDeliveredKeys(
        privateKey: String,
        derivationPath: String,
        callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit
    ) {
        UportHDSigner().computeAddressForPath(contex, privateKey, derivationPath, String.Empty, callback)
    }

    override fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>> {
        return try {
            val payload = JWTTools().decodeRaw(jwtToken).second
            handleTokenExpired(payload)
            Single.just(payload)
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
            Single.just(mapOf())
        }
    }

    override suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String {
        return JWTTools().createJWT(payload, getDIDKey(privateKey), KPSigner(privateKey))
    }

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
    }
}