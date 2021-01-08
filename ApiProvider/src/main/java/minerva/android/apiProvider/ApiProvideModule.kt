package minerva.android.apiProvider

import minerva.android.apiProvider.retrofit.RetrofitProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun apiProviderModule(isDebug: Boolean, baseUrl: String) = module {
    single(named(SERVICES_RETROFIT)) { RetrofitProvider.provideRetrofit(get(named(SERVICES_OKHTTP)), baseUrl) }
    single { RetrofitProvider.provideServicesApi(get(named(SERVICES_RETROFIT))) }
    single { RetrofitProvider.provideCoinGecko(get(named(SERVICES_RETROFIT))) }
    single(named(SERVICES_OKHTTP)) { RetrofitProvider.providePrivateOkHttpClient(isDebug) }
}

const val SERVICES_RETROFIT = "services_retrofit"
const val SERVICES_OKHTTP = "services_okhttp"