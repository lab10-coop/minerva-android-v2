package com.exchangemarketsprovider.retrofit

import com.exchangemarketsprovider.api.BinanceApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    fun providePrivateOkHttpClient(
        isDebug: Boolean
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (isDebug) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        return builder
            .connectTimeout(TIMEOUT_TIME, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_TIME, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_TIME, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(converterFactory)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()

    fun provideMinervaApi(retrofit: Retrofit): BinanceApi = retrofit.create(BinanceApi::class.java)

    private val gson: Gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val converterFactory = GsonConverterFactory.create(gson)

    private const val TIMEOUT_TIME: Long = 3
}