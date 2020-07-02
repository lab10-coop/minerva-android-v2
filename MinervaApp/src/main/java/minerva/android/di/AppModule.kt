package minerva.android.di

import minerva.android.BuildConfig
import minerva.android.edit.EditOrderViewModel
import minerva.android.identities.IdentitiesViewModel
import minerva.android.identities.edit.EditIdentityViewModel
import minerva.android.main.MainViewModel
import minerva.android.onboarding.create.CreateWalletViewModel
import minerva.android.onboarding.restore.RestoreWalletViewModel
import minerva.android.payment.PaymentRequestViewModel
import minerva.android.services.ServicesViewModel
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.services.login.scanner.LoginScannerViewModel
import minerva.android.settings.SettingsViewModel
import minerva.android.settings.backup.BackupViewModel
import minerva.android.splash.SplashScreenViewModel
import minerva.android.values.ValuesViewModel
import minerva.android.values.address.ValueAddressViewModel
import minerva.android.values.akm.SafeAccountSettingsViewModel
import minerva.android.values.create.NewValueViewModel
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.walletActions.WalletActionsViewModel
import minerva.android.walletmanager.createWalletManagerModules
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAppModule() = mutableListOf<Module>().apply {
    addAll(createWalletManagerModules(BuildConfig.DEBUG, BuildConfig.REST_API_URL, BuildConfig.BINANCE_URL))
    add(appModules)
}

private val appModules = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { SplashScreenViewModel(get()) }
    viewModel { BackupViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { IdentitiesViewModel(get(), get()) }
    viewModel { ValuesViewModel(get(), get(), get(), get()) }
    viewModel { EditIdentityViewModel(get(), get()) }
    viewModel { RestoreWalletViewModel(get()) }
    viewModel { CreateWalletViewModel(get()) }
    viewModel { ChooseIdentityViewModel(get(), get()) }
    viewModel { LoginScannerViewModel(get()) }
    viewModel { ValueAddressViewModel(get()) }
    viewModel { SafeAccountSettingsViewModel(get(), get()) }
    viewModel { TransactionsViewModel(get(), get(), get()) }
    viewModel { NewValueViewModel(get(), get()) }
    viewModel { ServicesViewModel(get()) }
    viewModel { WalletActionsViewModel(get()) }
    viewModel { PaymentRequestViewModel(get(), get(), get()) }
    viewModel { EditOrderViewModel(get()) }
}
