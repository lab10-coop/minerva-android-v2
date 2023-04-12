package minerva.android.walletmanager

import android.content.Context
import androidx.room.Room
import minerva.android.apiProvider.apiProviderModule
import minerva.android.blockchainprovider.createBlockchainProviderModule
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.configProvider.localSharedPrefs
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.walletConnect.walletConnectModules
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.keystore.KeyStoreManager
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.keystore.KeystoreRepositoryImpl
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.AccountManagerImpl
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManagerImpl
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.identity.IdentityManagerImpl
import minerva.android.walletmanager.manager.networks.NetworkManager.gasPriceMap
import minerva.android.walletmanager.manager.networks.NetworkManager.httpsUrlMap
import minerva.android.walletmanager.manager.networks.NetworkManager.wssUrlMap
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.manager.order.OrderManagerImpl
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.manager.services.ServiceManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManagerImpl
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.provider.CurrentTimeProviderImpl
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.provider.UnsupportedNetworkRepositoryImpl
import minerva.android.walletmanager.repository.asset.AssetBalanceRepository
import minerva.android.walletmanager.repository.asset.AssetBalanceRepositoryImpl
import minerva.android.walletmanager.repository.dapps.DappsRepository
import minerva.android.walletmanager.repository.dapps.DappsRepositoryImpl
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.repository.seed.MasterSeedRepositoryImpl
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepositoryImpl
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.LocalStorageImpl
import minerva.android.walletmanager.storage.RateStorage
import minerva.android.walletmanager.storage.RateStorageImpl
import minerva.android.walletmanager.utils.EnsProvider
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProviderImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, restApiUrl: String, marketsApiUrl: String, token: String) =
    createWalletModules()
        .plus(walletConnectModules)
        .plus(createCryptographyModules())
        .plus(createWalletConfigProviderModule(isDebug, restApiUrl, token))
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
    factory<AccountManager> { AccountManagerImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<TokenManager> { TokenManagerImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<ServiceManager> { ServiceManagerImpl(get(), get(), get()) }
    factory<MasterSeedRepository> { MasterSeedRepositoryImpl(get(), get()) }
    single<AssetBalanceRepository> {AssetBalanceRepositoryImpl()  }
    factory<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<WalletActionsRepository> { WalletActionsRepositoryImpl(get(), get(), get()) }
    factory<SafeAccountRepository> { SafeAccountRepositoryImpl(get(), get(), get(), get(), get()) }
    factory<OrderManager> { OrderManagerImpl(get()) }
    factory<CurrentTimeProvider> { CurrentTimeProviderImpl() }
    factory<UnsupportedNetworkRepository> { UnsupportedNetworkRepositoryImpl(get()) }
    single<WalletConnectRepository> { WalletConnectRepositoryImpl(get(), get(), get()) }
    single {
        Room.databaseBuilder(androidContext(), MinervaDatabase::class.java, "minerva_database")
            .fallbackToDestructiveMigration().build()
    }
    single<RateStorage> { RateStorageImpl() }
    single { get<MinervaDatabase>().dappDao() }
    single { get<MinervaDatabase>().favoriteDappDao() }
    single<DappsRepository> { DappsRepositoryImpl(get(), get(), get(), get()) }
}

private const val MinervaStorage = "MinervaSharedPrefs"
private const val minervaSharedPrefs = "minerva"
private const val localStorage = "LocalStorage"