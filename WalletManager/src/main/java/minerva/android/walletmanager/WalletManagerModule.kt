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
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.AccountManagerImpl
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.identity.IdentityManagerImpl
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.order.OrderManagerImpl
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.manager.services.ServiceManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManagerImpl
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.repository.seed.MasterSeedRepositoryImpl
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.smartContract.SmartContractRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.LocalStorageImpl
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProviderImpl
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
    .plus(createBlockchainProviderModule(NetworkManager.urlMap, BuildConfig.ENS_ADDRESS, NetworkManager.gasPriceMap))
    .plus(createExchangeRateProviderModule(isDebug, binanceUrl))

fun createWalletModules() = module {
    factory(named(localSharedPrefs)) { androidContext().getSharedPreferences(LocalStorage, Context.MODE_PRIVATE) }
    factory(named(minervaSharedPrefs)) { androidContext().getSharedPreferences(MinervaStorage, Context.MODE_PRIVATE) }
    factory { KeyStoreManager() }
    factory<KeystoreRepository> { KeystoreRepositoryImpl(get(named(minervaSharedPrefs)), get()) }
    factory<LocalStorage> { LocalStorageImpl(get(named(localSharedPrefs))) }
    factory<LocalWalletConfigProvider> { LocalWalletConfigProviderImpl(get(named(localSharedPrefs))) }
    factory<WalletConfigRepository> { WalletConfigRepositoryImpl(get(), get(), get()) }
    single<WalletConfigManager> { WalletConfigManagerImpl(get(), get()) }
    factory<IdentityManager> { IdentityManagerImpl(get(), get()) }
    factory<AccountManager> { AccountManagerImpl(get(), get(), get()) }
    factory<ServiceManager> { ServiceManagerImpl(get(), get(), get()) }
    factory<MasterSeedRepository> { MasterSeedRepositoryImpl(get(), get(), get()) }
    factory<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get()) }
    factory<WalletActionsRepository> { WalletActionsRepositoryImpl(get(), get(), get()) }
    factory<SmartContractRepository> { SmartContractRepositoryImpl(get(), get(), get(), get()) }
    factory<LocalWalletActionsConfigProvider> { LocalWalletActionsConfigProviderImpl(get(named(localSharedPrefs))) }
    factory<OrderManager> { OrderManagerImpl(get()) }
}

private const val LocalStorage = "LocalStorage"
private const val localSharedPrefs = "local"

private const val MinervaStorage = "MinervaSharedPrefs"
private const val minervaSharedPrefs = "minerva"