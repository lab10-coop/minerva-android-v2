package minerva.android.cryptographyProvider.repository

interface CryptographyRepository {
    fun createMasterKeys(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun showMnemonicForMasterKey(privateMasterKey: String, prompt: String, callback: (error: Exception?, mnemonic: String) -> Unit)
    fun importMasterKeysFromMnemonic(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun computeDeliveredKeys(
        privateMasterKey: String,
        derivationPath: String,
        prompt: String,
        callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit
    )

    fun validateMnemonic(mnemonic: String): List<String>
}