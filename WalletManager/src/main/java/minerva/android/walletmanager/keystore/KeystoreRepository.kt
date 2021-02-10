package minerva.android.walletmanager.keystore

import minerva.android.walletmanager.model.wallet.MasterSeed

interface KeystoreRepository {
    fun isMasterSeedSaved(): Boolean
    fun encryptMasterSeed(masterSeed: MasterSeed)
    fun decryptMasterSeed(): MasterSeed?
}