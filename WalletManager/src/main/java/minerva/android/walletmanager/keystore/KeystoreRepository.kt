package minerva.android.walletmanager.keystore

import minerva.android.walletmanager.model.MasterKey

interface KeystoreRepository {
    fun isMasterKeySaved(): Boolean
    fun encryptKey(masterKey: MasterKey)
    fun decryptKey(): MasterKey
}