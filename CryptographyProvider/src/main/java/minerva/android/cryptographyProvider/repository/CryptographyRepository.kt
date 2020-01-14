package minerva.android.cryptographyProvider.repository

interface CryptographyRepository {
    fun createMasterKey(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun validateMnemonic(mnemonic: String): List<String>
    fun showMnemonicForMasterKey(privateKey: String, prompt: String, callback: (error: Exception?, mnemonic: String) -> Unit)
    fun computeDeliveredKeys(
        privateKey: String,
        derivationPath: String,
        callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit
    )
}