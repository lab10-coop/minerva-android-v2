package minerva.android.cryptographyProvider.repository

import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.model.DerivedKeys

interface CryptographyRepository {
    fun createMasterSeed(derivationPath: String): Single<Triple<String, String, String>> //seed, publicKey, privateKey
    fun getMnemonicForMasterSeed(seed: String): String
    fun calculateDerivedKeys(seed: String, index: Int, derivationPathPrefix: String, isTestNet: Boolean = true): Single<DerivedKeys>
    fun restoreMasterSeed(
        mnemonic: String,
        derivationPath: String
    ): Single<Triple<String, String, String>> //seed, publicKey, privateKey

    fun validateMnemonic(mnemonic: String): List<String>
    fun decodeJwtToken(jwtToken: String): Single<Map<String, Any?>>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String>
}