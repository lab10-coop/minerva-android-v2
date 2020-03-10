package minerva.android.walletmanager

import com.exchangemarketsprovider.createExchangeRateProviderModule
import minerva.android.blockchainprovider.createBlockchainProviderModule
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.servicesApiProvider.createServicesApiProviderModule
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.keystore.KeystoreRepositoryImpl
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.manager.SmartContractManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.wallet.WalletManagerImpl
import minerva.android.walletmanager.manager.wallet.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.manager.wallet.walletconfig.localProvider.LocalWalletConfigProviderImpl
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepository
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepositoryImpl
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.manager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.manager.walletActions.localProvider.LocalWalletActionsConfigProviderImpl
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.LocalStorageImpl
import org.koin.dsl.module

fun createWalletManagerModules(isDebug: Boolean, baseUrl: String, binanceUrl: String) = createWalletModules()
    .plus(createCryptographyModules())
    .plus(createWalletConfigProviderModule(isDebug, baseUrl))
    .plus(createServicesApiProviderModule(isDebug, baseUrl))
    .plus(createBlockchainProviderModule(Network.urlMap, BuildConfig.ENS_ADDRESS))
    .plus(createExchangeRateProviderModule(isDebug, binanceUrl))

fun createWalletModules() = module {
    factory { KeystoreRepositoryImpl(get()) as KeystoreRepository }
    factory { LocalWalletConfigProviderImpl(get()) as LocalWalletConfigProvider }
    factory { WalletConfigRepositoryImpl(get(), get(), get(), get()) as WalletConfigRepository }
    factory { LocalStorageImpl(get()) as LocalStorage }
    single { WalletManagerImpl(get(), get(), get(), get(), get(), get(), get()) as WalletManager }
    factory { LocalWalletActionsConfigProviderImpl(get()) as LocalWalletActionsConfigProvider }
    factory { WalletActionsRepositoryImpl(get(), get()) as WalletActionsRepository }
    factory { SmartContractManagerImpl(get()) as SmartContractManager }
}