package minerva.android.walletConnect

import minerva.android.walletConnect.providers.OkHttpProvider
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletConnect.repository.WalletConnectRepositoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

val walletConnectModules = module {
    factory(named(QUALIFIER)) { OkHttpProvider.okHttpClient }
    factory<WalletConnectRepository> { WalletConnectRepositoryImpl(get(named(QUALIFIER))) }
}

private const val QUALIFIER = "WalletConnect"