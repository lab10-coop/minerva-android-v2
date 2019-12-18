package minerva.android.walletmanager

import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.manager.WalletManagerImpl
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))

fun createWalletModules() = module {
    single { KeystoreRepository(get()) }
    single { WalletManagerImpl(get(), get(), get()) as WalletManager }
}