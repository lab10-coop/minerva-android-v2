package minerva.android.di

import minerva.android.walletmanager.WalletManager
import minerva.android.walletmanager.WalletManagerImpl
import minerva.android.walletmanager.keystore.KeystoreRepository
import org.koin.core.module.Module
import org.koin.dsl.module


fun createAppModule() = mutableListOf<Module>().apply {
    add(walletManagerModule)
}

private val walletManagerModule = module {
    single { KeystoreRepository(get()) }
    single { WalletManagerImpl(get(), get()) as WalletManager }
}
