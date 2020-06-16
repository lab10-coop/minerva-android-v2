package minerva.android.walletmanager.repository.seed

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.RestoreWalletResponse

interface MasterSeedRepository {
    fun validateMnemonic(mnemonic: String): List<String>
    fun getMnemonic(): String
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun isMasterSeedAvailable(): Boolean
    fun createMasterSeed(): Completable
    fun restoreMasterSeed(mnemonic: String): Single<RestoreWalletResponse>
    fun initWalletConfig()
    fun dispose()
    fun getValueIterator(): Int
}