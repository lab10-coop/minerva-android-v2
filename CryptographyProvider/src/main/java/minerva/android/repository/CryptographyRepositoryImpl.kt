package minerva.android.repository

import android.content.Context
import com.uport.sdk.signer.UportHDSigner
import com.uport.sdk.signer.encryption.KeyProtection

class CryptographyRepositoryImpl(var contex: Context) : CryptographyRepository {

    override fun createMasterKeys(callback: (error: Exception?, rootAddress: String, rootPublicKey: String) -> Unit) {
        UportHDSigner().createHDSeed(contex, KeyProtection.Level.SIMPLE, callback)
    }

    override fun showMnemonicForMasterKey(privateMasterKey: String, prompt: String, callback: (error: Exception?, seedPhase: String) -> Unit) {
        UportHDSigner().showHDSeed(contex, privateMasterKey, prompt, callback)
    }

    override fun importMasterKeysFromMnemonic(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        UportHDSigner().importHDSeed(contex, KeyProtection.Level.SIMPLE, mnemonic, callback)
    }

    override fun computeDeliveredKeys(
        privateMasterKey: String,
        derivationPath: String,
        prompt: String,
        callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit
    ) {
        UportHDSigner().computeAddressForPath(contex, privateMasterKey, derivationPath, prompt, callback)
    }
}