package minerva.android.configProvider

import minerva.android.configProvider.retrofit.RetrofitProvider
import org.koin.dsl.module

fun createWalletConfigProviderModule(isDebug: Boolean, baseUrl: String) = module {
    single { RetrofitProvider.provideRetrofit(get(), baseUrl) }
    single { RetrofitProvider.provideMinervaApi(get()) }
    single { RetrofitProvider.providePrivateOkHttpClient(isDebug, get()) }
    single { RetrofitProvider.provideUserHeaderInterceptor() }
}