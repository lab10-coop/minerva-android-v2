package minerva.android.di

import minerva.android.BuildConfig
import minerva.android.identities.EditIdentityFragment
import minerva.android.identities.EditIdentityViewModel
import minerva.android.onboarding.create.CreateWalletViewModel
import minerva.android.walletmanager.createWalletManagerModules
import minerva.android.identities.IdentitiesViewModel
import minerva.android.main.MainViewModel
import minerva.android.onboarding.restore.RestoreWalletViewModel
import minerva.android.services.login.PainlessLoginViewModel
import minerva.android.settings.SettingsViewModel
import minerva.android.settings.backup.BackupViewModel
import minerva.android.values.ValueAddressViewModel
import minerva.android.values.ValuesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManagerModules(BuildConfig.DEBUG, BuildConfig.REST_API_URL))
    add(appModules)
}

private val appModules = module {
    viewModel { MainViewModel(get()) }
    viewModel { BackupViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { IdentitiesViewModel(get()) }
    viewModel { ValuesViewModel(get()) }
    viewModel { EditIdentityViewModel(get()) }
    viewModel { RestoreWalletViewModel(get()) }
    viewModel { CreateWalletViewModel(get()) }
    viewModel { PainlessLoginViewModel(get()) }
    viewModel { ValueAddressViewModel(get()) }
}
