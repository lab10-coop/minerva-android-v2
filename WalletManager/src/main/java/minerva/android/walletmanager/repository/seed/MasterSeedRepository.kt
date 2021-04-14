package minerva.android.walletmanager.repository.seed

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.wallet.WalletConfig

interface MasterSeedRepository {
    val walletConfigLiveData: LiveData<Event<WalletConfig>>
    val walletConfigErrorLiveData: LiveData<Event<Throwable>>
    fun validateMnemonic(mnemonic: String): List<String>
    fun getMnemonic(): String
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun isMasterSeedAvailable(): Boolean
    fun createWalletConfig(): Completable
    fun restoreMasterSeed(mnemonic: String): Completable
    fun initWalletConfig()
    fun dispose()
    fun getAccountIterator(): Int
    val isBackupAllowed: Boolean
    val isSynced: Boolean
    var areMainNetworksEnabled: Boolean
    //var toggleMainNetsEnabled: Boolean
}