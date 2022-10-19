package minerva.android.walletmanager.repository.seed

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.wallet.MasterKeys
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig

interface MasterSeedRepository {
    val walletConfigLiveData: LiveData<Event<WalletConfig>>
    val walletConfigErrorLiveData: LiveData<Event<Throwable>>
    val isBackupAllowed: Boolean
    val isSynced: Boolean
    var areMainNetworksEnabled: Boolean

    fun areMnemonicWordsValid(mnemonic: String): Boolean
    fun getMnemonic(): String
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun isMasterSeedAvailable(): Boolean
    fun createWalletConfig(masterSeed: MasterSeed? = null): Completable
    fun restoreMasterSeed(mnemonicAndPassword: String): MasterKeys
    fun restoreWalletConfig(mnemonicAndPassword: MasterSeed): Completable
    fun restoreWalletConfigWithSavedMasterSeed(): Completable
    fun initWalletConfig()
    fun dispose()
    fun getAccountIterator(): Int
    fun getWalletConfig(): WalletConfig
}