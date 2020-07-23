package minerva.android.walletmanager.keystore

import minerva.android.walletmanager.model.MasterSeed

interface KeystoreRepository {
    fun isMasterSeedSaved(): Boolean
    fun encryptMasterSeed(masterSeed: MasterSeed)
    fun decryptMasterSeed(): MasterSeed?
}