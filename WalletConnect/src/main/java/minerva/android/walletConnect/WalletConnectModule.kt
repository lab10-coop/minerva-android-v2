package minerva.android.walletConnect

import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.providers.OkHttpProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

val walletConnectModules = module {
    factory(named(QUALIFIER)) { OkHttpProvider.okHttpClient }
    factory { WCClient(get(named(QUALIFIER))) }
}

private const val QUALIFIER = "WalletConnect"