package minerva.android.di

import minerva.android.walletmanager.createWalletManageModules
import org.koin.core.module.Module
import org.koin.dsl.module


fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManageModules())
    add(appModules)
}

private val appModules = module {
    //    todo inject viewModels
}
