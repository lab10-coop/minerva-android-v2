package minerva.android.walletmanager

import android.content.Context
import android.util.Log
import minerva.android.walletmanager.keystore.KeystoreRepository

interface WalletManager {
    fun isMasterKeyAvailable(): Boolean
}

class WalletManagerImpl(private val context: Context, private val keyStoreRepository: KeystoreRepository) : WalletManager {

    override fun isMasterKeyAvailable(): Boolean = keyStoreRepository.isMasterKeySaved()
}