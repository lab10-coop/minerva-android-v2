package minerva.android.cryptographyProvider.repository

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject

interface CryptographyRepository {
    fun createMasterKey(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun validateMnemonic(mnemonic: String): List<String>
    fun showMnemonicForMasterKey(privateKey: String, prompt: String, callback: (error: Exception?, mnemonic: String) -> Unit)
    fun computeDeliveredKeys(privateKey: String, index: Int): Single<Triple<Int, String, String>>

    fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>>
    suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String
}