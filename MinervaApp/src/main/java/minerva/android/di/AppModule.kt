package minerva.android.di

import android.content.Context
import minerva.android.BuildConfig
import minerva.android.accounts.address.AddressViewModel
import minerva.android.accounts.akm.SafeAccountSettingsViewModel
import minerva.android.accounts.create.NewAccountViewModel
import minerva.android.accounts.transaction.fragment.AccountsViewModel
import minerva.android.accounts.transaction.fragment.TransactionViewModel
import minerva.android.accounts.walletconnect.WalletConnectViewModel
import minerva.android.edit.EditOrderViewModel
import minerva.android.identities.MinervaPrimitivesViewModel
import minerva.android.identities.edit.EditIdentityViewModel
import minerva.android.integration.ThirdPartyRequestViewModel
import minerva.android.main.MainViewModel
import minerva.android.walletmanager.storage.TempStorage
import minerva.android.walletmanager.storage.TempStorageImpl
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.token.AddTokenViewModel
import minerva.android.token.ManageTokensViewModel
import minerva.android.onboarding.create.CreateWalletViewModel
import minerva.android.onboarding.restore.RestoreWalletViewModel
import minerva.android.services.ServicesViewModel
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.services.login.scanner.LoginScannerViewModel
import minerva.android.settings.SettingsViewModel
import minerva.android.settings.authentication.AuthenticationViewModel
import minerva.android.settings.backup.BackupViewModel
import minerva.android.splash.SplashScreenViewModel
import minerva.android.walletActions.WalletActionsViewModel
import minerva.android.walletmanager.createWalletManagerModules
import minerva.android.widget.clubCard.CacheStorage
import minerva.android.widget.clubCard.CacheStorageImpl
import minerva.android.widget.clubCard.ClubCardViewModel
import minerva.android.widget.state.AppUIState
import minerva.android.widget.state.AppUIStateImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun createAppModule() = mutableListOf<Module>().apply {
    addAll(
        createWalletManagerModules(
            BuildConfig.DEBUG,
            BuildConfig.REST_API_URL,
            BuildConfig.MARKETS_API_URL,
            BuildConfig.API_TOKEN
        )
    )
    add(appModules)
}

private val appModules = module {
    single<AppUIState> { AppUIStateImpl() }
    factory { androidContext().getSharedPreferences(MinervaCache, Context.MODE_PRIVATE) }
    factory<CacheStorage> { CacheStorageImpl(get()) }
    viewModel { ClubCardViewModel(get()) }
    viewModel { MainViewModel(get(), get(), get(), get(), get()) }
    viewModel { WalletConnectInteractionsViewModel(get(), get()) }
    viewModel { SplashScreenViewModel(get()) }
    viewModel { BackupViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { MinervaPrimitivesViewModel(get(), get()) }
    viewModel { AccountsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { EditIdentityViewModel(get(), get()) }
    viewModel { RestoreWalletViewModel(get()) }
    viewModel { CreateWalletViewModel(get()) }
    viewModel { ChooseIdentityViewModel(get(), get()) }
    viewModel { LoginScannerViewModel(get(), get(), get()) }
    viewModel { AddressViewModel(get(), get()) }
    viewModel { SafeAccountSettingsViewModel(get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get()) }
    viewModel { NewAccountViewModel(get(), get()) }
    viewModel { ServicesViewModel(get(), get(), get()) }
    viewModel { WalletActionsViewModel(get()) }
    viewModel { ThirdPartyRequestViewModel(get(), get(), get()) }
    viewModel { EditOrderViewModel(get()) }
    viewModel { WalletConnectViewModel(get(), get()) }
    viewModel { ManageTokensViewModel(get(), get(), get()) }
    viewModel { AddTokenViewModel(get(), get(), get()) }
    viewModel { AuthenticationViewModel(get()) }
}

private const val MinervaCache = "MinervaCache"