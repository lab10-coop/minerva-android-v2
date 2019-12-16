package minerva.android.di

import minerva.android.onBoarding.restore.RestoreWalletViewModel
import minerva.android.walletmanager.createWalletManageModules
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module


fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManageModules())
    add(appModules)
}

private val appModules = module {
    viewModel { RestoreWalletViewModel(get()) }
}
