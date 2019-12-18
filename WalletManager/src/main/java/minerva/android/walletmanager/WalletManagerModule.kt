package minerva.android.walletmanager

import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.manager.WalletManagerImpl
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProvider
import minerva.android.walletmanager.walletconfig.OnlineWalletConfigProvider
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import minerva.android.walletmanager.walletconfig.mock.LocalWalletConfigProviderMock
import minerva.android.walletmanager.walletconfig.mock.OnlineWalletConfigProviderMock
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))

fun createWalletModules() = module {
    factory { KeystoreRepository(get()) }
    factory { LocalWalletConfigProviderMock() as LocalWalletConfigProvider }
    factory { OnlineWalletConfigProviderMock() as OnlineWalletConfigProvider }
    factory { WalletConfigRepository(get(), get()) }
    single { WalletManagerImpl(get(), get(), get(), get()) as WalletManager }
}