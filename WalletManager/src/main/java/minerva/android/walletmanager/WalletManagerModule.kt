package minerva.android.walletmanager

import com.exchangemarketprovider.createExchangeRateProviderModule
import minerva.android.blockchainprovider.createBlockchainProviderModule
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.servicesApiProvider.createServicesApiProviderModule
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.manager.WalletManagerImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProvider
import minerva.android.walletmanager.walletconfig.LocalWalletConfigProviderImpl
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String, blockchainUrl: String, binanceUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))
    .plus(createServicesApiProviderModule(isDebug, baseUrl))
    .plus(createBlockchainProviderModule(blockchainUrl))
    .plus(createExchangeRateProviderModule(isDebug, binanceUrl))

fun createWalletModules() = module {
    factory { KeystoreRepository(get()) }
    factory { LocalWalletConfigProviderImpl(get()) as LocalWalletConfigProvider }
    factory { WalletConfigRepository(get(), get(), get(), get()) }
    factory { LocalStorage(get()) }
    single { WalletManagerImpl(get(), get(), get(), get(), get(), get(), get()) as WalletManager }
}