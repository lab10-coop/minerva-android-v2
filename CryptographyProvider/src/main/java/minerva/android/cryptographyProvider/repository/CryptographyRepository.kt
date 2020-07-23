package minerva.android.cryptographyProvider.repository

import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.model.DerivedKeys

interface CryptographyRepository {
    fun createMasterSeed(): Single<Triple<String, String, String>>
    fun getMnemonicForMasterSeed(seed: String): String
    fun computeDeliveredKeys(seed: String, index: Int): Single<DerivedKeys>
    fun restoreMasterSeed(mnemonic: String): Single<Triple<String, String, String>>
    fun validateMnemonic(mnemonic: String): List<String>
    fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String>
}