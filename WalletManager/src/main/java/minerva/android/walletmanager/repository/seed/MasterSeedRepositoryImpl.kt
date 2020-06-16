package minerva.android.walletmanager.repository.seed

import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.RestoreWalletResponse
import minerva.android.walletmanager.storage.LocalStorage

class MasterSeedRepositoryImpl(
    private val walletConfigManager: WalletConfigManager,
    private val localStorage: LocalStorage,
    private val cryptographyRepository: CryptographyRepository
) : MasterSeedRepository {

    override fun isMasterSeedAvailable(): Boolean = walletConfigManager.isMasterSeedSaved()

    override fun restoreMasterSeed(mnemonic: String): Single<RestoreWalletResponse> =
        cryptographyRepository.restoreMasterSeed(mnemonic).map { it.run { MasterSeed(first, second, third) } }
            .flatMap { walletConfigManager.getWalletConfig(MasterSeed(it.seed, it.publicKey, it.privateKey)) }

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean = localStorage.isMnemonicRemembered()

    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

    override fun createMasterSeed() =
        cryptographyRepository.createMasterSeed().map { it.run { MasterSeed(first, second, third) } }
            .flatMapCompletable { walletConfigManager.createWalletConfig(it) }

    override fun getMnemonic() = cryptographyRepository.getMnemonicForMasterSeed(walletConfigManager.masterSeed.seed)

    override fun initWalletConfig() {
        walletConfigManager.initWalletConfig()
    }

    override fun dispose() {
        walletConfigManager.dispose()
    }

    override fun getValueIterator(): Int = walletConfigManager.getValueIterator()
}