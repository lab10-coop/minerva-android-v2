package minerva.android.walletmanager

import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.manager.WalletManagerImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProvider
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProviderImpl
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))

fun createWalletModules() = module {
    factory { KeystoreRepository(get()) }
    factory { LocalWalletConfigProviderImpl(get()) as LocalWalletConfigProvider }
    factory { WalletConfigRepository(get(), get()) }
    factory { LocalStorage(get()) }
    single { WalletManagerImpl(get(), get(), get(), get()) as WalletManager }
}