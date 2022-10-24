package minerva.android.cryptographyProvider.repository

import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.cryptographyProvider.repository.model.Seed

interface CryptographyRepository {
    /*seed, publicKey, privateKey*/
    fun createMasterSeed(): Single<Triple<String, String, String>>
    fun getMnemonicForMasterSeed(seed: String): String
    fun calculateDerivedKeysSingle(
        seed: String,
        password: String,
        index: Int,
        derivationPathPrefix: String,
        isTestNet: Boolean = true
    ): Single<DerivedKeys>

    fun calculateDerivedKeys(
        seed: String,
        password: String,
        index: Int,
        derivationPathPrefix: String,
        isTestNet: Boolean = true
    ): DerivedKeys

    fun restoreMasterSeed(mnemonicAndPassword: String): Seed
    fun areMnemonicWordsValid(mnemonicAndPassword: String): Boolean
    fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String>
}