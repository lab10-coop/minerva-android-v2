package minerva.android.walletmanager

import minerva.android.walletmanager.keystore.KeystoreRepository

interface WalletManager {
    fun masterKey(): String?
}

class WalletManagerImpl(private val keyStoreRepository: KeystoreRepository) : WalletManager {

    override fun masterKey(): String? = keyStoreRepository.masterKey


}