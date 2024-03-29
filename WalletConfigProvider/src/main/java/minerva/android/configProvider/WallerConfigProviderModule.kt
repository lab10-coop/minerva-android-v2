package minerva.android.configProvider

import minerva.android.configProvider.localProvider.LocalWalletConfigProvider
import minerva.android.configProvider.localProvider.LocalWalletConfigProviderImpl
import minerva.android.configProvider.logger.Logger
import minerva.android.configProvider.logger.LoggerImpl
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.configProvider.repository.MinervaApiRepositoryImpl
import minerva.android.configProvider.retrofit.RetrofitProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createWalletConfigProviderModule(isDebug: Boolean, baseUrl: String, token: String) = module {
    single { RetrofitProvider.provideRetrofit(get(), baseUrl) }
    single { RetrofitProvider.provideMinervaApi(get()) }
    single { RetrofitProvider.providePrivateOkHttpClient(isDebug, get()) }
    single { RetrofitProvider.provideUserHeaderInterceptor(token) }
    single<Logger> { LoggerImpl() }
    single<MinervaApiRepository> { MinervaApiRepositoryImpl(get(), get()) }
    factory<LocalWalletConfigProvider> { LocalWalletConfigProviderImpl(get(named(localSharedPrefs))) }
}

const val localSharedPrefs = "local"