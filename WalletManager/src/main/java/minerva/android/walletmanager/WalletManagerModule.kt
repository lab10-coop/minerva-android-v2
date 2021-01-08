package minerva.android.walletmanager

import android.content.Context
import minerva.android.blockchainprovider.createBlockchainProviderModule
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.apiProvider.apiProviderModule
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
import minerva.android.configProvider.localSharedPrefs
import minerva.android.walletmanager.manager.networks.NetworkManager.gasPriceMap
import minerva.android.walletmanager.manager.networks.NetworkManager.httpsUrlMap
import minerva.android.walletmanager.manager.networks.NetworkManager.wssUrlMap
import minerva.android.walletmanager.utils.EnsProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, restApiUrl: String, marketsApiUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, restApiUrl))
    .plus(apiProviderModule(isDebug, marketsApiUrl))
    .plus(createBlockchainProviderModule(httpsUrlMap, gasPriceMap, wssUrlMap))

fun createWalletModules() = module {
    factory { EnsProvider(get()).ensUrl }
    factory(named(localSharedPrefs)) { androidContext().getSharedPreferences(localStorage, Context.MODE_PRIVATE) }
    factory(named(minervaSharedPrefs)) { androidContext().getSharedPreferences(MinervaStorage, Context.MODE_PRIVATE) }
    factory { KeyStoreManager() }
    factory<KeystoreRepository> { KeystoreRepositoryImpl(get(named(minervaSharedPrefs)), get()) }
    factory<LocalStorage> { LocalStorageImpl(get(named(localSharedPrefs))) }
    factory<LocalWalletActionsConfigProvider> { LocalWalletActionsConfigProviderImpl(get(named(localSharedPrefs))) }
    single<WalletConfigManager> { WalletConfigManagerImpl(get(), get(), get(), get(), get()) }
    factory<IdentityManager> { IdentityManagerImpl(get(), get(), get()) }
    factory<AccountManager> { AccountManagerImpl(get(), get(), get()) }
    factory<ServiceManager> { ServiceManagerImpl(get(), get(), get()) }
    factory<MasterSeedRepository> { MasterSeedRepositoryImpl(get(), get()) }
    factory<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get(), get()) }
    factory<WalletActionsRepository> { WalletActionsRepositoryImpl(get(), get(), get()) }
    factory<SmartContractRepository> { SmartContractRepositoryImpl(get(), get(), get(), get()) }
    factory<OrderManager> { OrderManagerImpl(get()) }
}

private const val MinervaStorage = "MinervaSharedPrefs"
private const val minervaSharedPrefs = "minerva"
private const val localStorage = "LocalStorage"