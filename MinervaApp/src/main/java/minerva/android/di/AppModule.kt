package minerva.android.di

import minerva.android.BuildConfig
import minerva.android.onBoarding.restore.RestoreWalletViewModel
import minerva.android.walletmanager.createWalletManagerModules
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManagerModules(BuildConfig.DEBUG, BuildConfig.REST_API_URL))
    add(appModules)
}

private val appModules = module {
    viewModel { RestoreWalletViewModel(get()) }
}
