package minerva.android.di

import minerva.android.BuildConfig
import minerva.android.walletmanager.createWalletManagerModules
import minerva.android.identities.IdentitiesViewModel
import minerva.android.main.MainViewModel
import minerva.android.onboarding.OnBoardingViewModel
import minerva.android.onboarding.restore.RestoreWalletViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module


fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManagerModules(BuildConfig.DEBUG, BuildConfig.REST_API_URL))
    add(appModules)
}

private val appModules = module {
    viewModel { MainViewModel(get()) }
    viewModel { OnBoardingViewModel(get()) }
    viewModel { IdentitiesViewModel(get()) }
    viewModel { RestoreWalletViewModel(get()) }
}
