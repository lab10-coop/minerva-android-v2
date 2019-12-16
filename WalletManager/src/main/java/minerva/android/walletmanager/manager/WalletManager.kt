package minerva.android.walletmanager.manager

import android.content.Context
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.keystore.KeystoreRepository

interface WalletManager {
    fun isMasterKeyAvailable(): Boolean
    fun validateMnemonic(mnemonic: String): List<String>
}

//TODO implement storing derivation path "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val context: Context,
    private val keyStoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository
) : WalletManager {

    override fun isMasterKeyAvailable(): Boolean = keyStoreRepository.isMasterKeySaved()
    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

}