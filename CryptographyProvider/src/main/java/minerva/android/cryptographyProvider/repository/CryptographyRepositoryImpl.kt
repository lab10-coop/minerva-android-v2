package minerva.android.cryptographyProvider.repository

import android.content.Context
import com.uport.sdk.signer.UportHDSigner
import com.uport.sdk.signer.encryption.KeyProtection
import minerva.android.kotlinUtils.Empty
import org.kethereum.bip39.wordlists.WORDLIST_ENGLISH
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

    override fun validateMnemonic(mnemonic: String): List<String> {
        mutableListOf<String>().apply {
            collectInvalidWords(StringTokenizer(mnemonic), this)
            return if (this.isEmpty()) {
                emptyList()
            } else {
                this
            }
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
}