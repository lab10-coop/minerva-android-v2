package minerva.android.servicesApiProvider

import minerva.android.servicesApiProvider.retrofit.ServicesRetrofitProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createServicesApiProviderModule(isDebug: Boolean, baseUrl: String) = module {
    single(named(SERVICES_RETROFIT)) { ServicesRetrofitProvider.provideRetrofit(get(named(
        SERVICES_OKHTTP
    )), baseUrl) }
    single { ServicesRetrofitProvider.provideServicesApi(get(named(SERVICES_RETROFIT))) }
    single(named(SERVICES_OKHTTP)) { ServicesRetrofitProvider.providePrivateOkHttpClient(isDebug) }
}

const val SERVICES_RETROFIT = "services_retrofit"
const val SERVICES_OKHTTP = "services_okhttp"