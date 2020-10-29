package minerva.android.walletmanager.repository.seed

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.RestoreWalletResponse
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage

class MasterSeedRepositoryImpl(
    private val walletConfigManager: WalletConfigManager,
    private val localStorage: LocalStorage,
    private val cryptographyRepository: CryptographyRepository
) : MasterSeedRepository {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override val walletConfigErrorLiveData: LiveData<Event<Throwable>>
        get() = walletConfigManager.walletConfigErrorLiveData

    override fun isMasterSeedAvailable(): Boolean = walletConfigManager.isMasterSeedSaved()

    override fun restoreMasterSeed(mnemonic: String): Single<RestoreWalletResponse> =
        cryptographyRepository.restoreMasterSeed(mnemonic)
            .flatMap { (seed, publicKey, privateKey) ->
                walletConfigManager.restoreWalletConfig(MasterSeed(seed, publicKey, privateKey))
            }

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean = localStorage.isMnemonicRemembered()

    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

    override fun createWalletConfig(): Completable =
        cryptographyRepository.createMasterSeed()
            .flatMapCompletable { (seed, publicKey, privateKey) ->
                walletConfigManager.createWalletConfig(MasterSeed(seed, publicKey, privateKey))
            }

    override fun getMnemonic() = cryptographyRepository.getMnemonicForMasterSeed(walletConfigManager.masterSeed.seed)

    override fun initWalletConfig() {
        walletConfigManager.initWalletConfig()
    }

    override fun dispose() {
        walletConfigManager.dispose()
    }

    override fun getValueIterator(): Int = walletConfigManager.getValueIterator()
}