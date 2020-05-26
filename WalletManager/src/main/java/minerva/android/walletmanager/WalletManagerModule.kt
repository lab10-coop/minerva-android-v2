package minerva.android.walletmanager

import android.content.Context
import com.exchangemarketsprovider.createExchangeRateProviderModule
import minerva.android.blockchainprovider.createBlockchainProviderModule
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.servicesApiProvider.createServicesApiProviderModule
import minerva.android.walletmanager.keystore.KeyStoreManager
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.keystore.KeystoreRepositoryImpl
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.manager.SmartContractManagerImpl
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProviderImpl
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.LocalStorageImpl
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.wallet.WalletManagerImpl
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProviderImpl
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepository
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String, binanceUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))
    .plus(createServicesApiProviderModule(isDebug, baseUrl))
    .plus(createBlockchainProviderModule(Network.urlMap, BuildConfig.ENS_ADDRESS, Network.gasPriceMap))
    .plus(createExchangeRateProviderModule(isDebug, binanceUrl))

fun createWalletModules() = module {
    factory(named(localSharedPrefs)) { androidContext().getSharedPreferences(LocalStorage, Context.MODE_PRIVATE) }
    factory(named(minervaSharedPrefs)) { androidContext().getSharedPreferences(MinervaStorage, Context.MODE_PRIVATE) }
    factory { KeyStoreManager() }
    factory<KeystoreRepository> { KeystoreRepositoryImpl(get(named(minervaSharedPrefs)), get()) }
    factory<LocalWalletConfigProvider> { LocalWalletConfigProviderImpl(get(named(localSharedPrefs))) }
    factory<WalletConfigRepository> { WalletConfigRepositoryImpl(get(), get(), get()) }
    factory<LocalStorage> { LocalStorageImpl(get(named(localSharedPrefs))) }
    single<WalletManager> { WalletManagerImpl(get(), get(), get(), get(), get(), get(), get()) }
    factory<LocalWalletActionsConfigProvider> { LocalWalletActionsConfigProviderImpl(get(named(localSharedPrefs))) }
    factory<WalletActionsRepository> { WalletActionsRepositoryImpl(get(), get()) }
    factory<SmartContractManager> { SmartContractManagerImpl(get(), get(), get()) }
}

private const val LocalStorage = "LocalStorage"
private const val localSharedPrefs = "local"

private const val MinervaStorage = "MinervaSharedPrefs"
private const val minervaSharedPrefs = "minerva"