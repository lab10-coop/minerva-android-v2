package minerva.android.walletmanager.repository.seed

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.RestoreWalletResponse
import minerva.android.walletmanager.model.WalletConfig

interface MasterSeedRepository {
    val walletConfigLiveData: LiveData<WalletConfig>
    val walletConfigErrorLiveData: LiveData<Event<Throwable>>
    fun validateMnemonic(mnemonic: String): List<String>
    fun getMnemonic(): String
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun isMasterSeedAvailable(): Boolean
    fun createWalletConfig(): Completable
    fun restoreMasterSeed(mnemonic: String): Single<RestoreWalletResponse>
    fun initWalletConfig()
    fun dispose()
    fun getValueIterator(): Int
}