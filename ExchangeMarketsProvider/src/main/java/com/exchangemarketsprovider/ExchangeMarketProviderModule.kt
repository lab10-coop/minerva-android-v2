package com.exchangemarketsprovider

import com.exchangemarketsprovider.retrofit.RetrofitProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun createExchangeRateProviderModule(isDebug: Boolean, baseUrl: String) = module {
    single(named(BINANCE_RETROFIT)) { RetrofitProvider.provideRetrofit(get(named(BINANCE_OKHTTP)), baseUrl) }
    single { RetrofitProvider.provideMinervaApi(get(named(BINANCE_RETROFIT))) }
    single(named(BINANCE_OKHTTP)) { RetrofitProvider.providePrivateOkHttpClient(isDebug) }
}

const val BINANCE_RETROFIT = "binance_retrofit"
const val BINANCE_OKHTTP = "binance_okhttp"