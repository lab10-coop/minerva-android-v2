package minerva.android.walletmanager

import minerva.android.cryptographyProvider.createCryptographyModules
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.manager.WalletManagerImpl
import org.koin.dsl.module

fun createWalletManageModules() = createWalletModules()
    .plus(createCryptographyModules())

fun createWalletModules() = module {
    single { KeystoreRepository(get()) }
    single { WalletManagerImpl(get(), get(), get()) as WalletManager }
}