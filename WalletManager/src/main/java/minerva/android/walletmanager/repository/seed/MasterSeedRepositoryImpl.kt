package minerva.android.walletmanager.repository.seed

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig

class MasterSeedRepositoryImpl(
    private val walletConfigManager: WalletConfigManager,
    private val cryptographyRepository: CryptographyRepository
) : MasterSeedRepository {

    override val walletConfigLiveData: LiveData<Event<WalletConfig>>
        get() = walletConfigManager.walletConfigLiveData

    override val walletConfigErrorLiveData: LiveData<Event<Throwable>>
        get() = walletConfigManager.walletConfigErrorLiveData

    override fun isMasterSeedAvailable(): Boolean = walletConfigManager.isMasterSeedSaved()

    override fun restoreMasterSeed(mnemonic: String): Completable =
        cryptographyRepository.restoreMasterSeed(mnemonic)
            .flatMapCompletable { (seed, publicKey, privateKey) ->
                walletConfigManager.restoreWalletConfig(MasterSeed(seed, publicKey, privateKey))
            }

    override fun saveIsMnemonicRemembered() {
        walletConfigManager.saveIsMnemonicRemembered()
    }

    override fun isMnemonicRemembered(): Boolean = walletConfigManager.isMnemonicRemembered()

    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

    override fun createWalletConfig(): Completable =
        cryptographyRepository.createMasterSeed()
            .flatMapCompletable { (seed, publicKey, privateKey) ->
                walletConfigManager.createWalletConfig(MasterSeed(seed, publicKey, privateKey))
            }

    override fun getMnemonic(): String =
        cryptographyRepository.getMnemonicForMasterSeed(walletConfigManager.masterSeed.seed)

    override fun initWalletConfig() {
        walletConfigManager.initWalletConfig()
    }

    override fun dispose() {
        walletConfigManager.dispose()
    }

    override fun getValueIterator(): Int = walletConfigManager.getValueIterator()

    override val isBackupAllowed: Boolean
        get() = walletConfigManager.isBackupAllowed

    override val isSynced: Boolean
        get() = walletConfigManager.isSynced

    override var areMainNetworksEnabled: Boolean
        get() = walletConfigManager.areMainNetworksEnabled
        set(value) {
            walletConfigManager.areMainNetworksEnabled = value
        }

//    override var toggleMainNetsEnabled: Boolean
//        get() = walletConfigManager.toggleMainNetsEnabled
//        set(value) {
//            walletConfigManager.toggleMainNetsEnabled = value
//        }
}