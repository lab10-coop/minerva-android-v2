package com.exchangemarketsprovider.api

import com.exchangemarketsprovider.model.Market
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {

    @GET("/api/v3/ticker/price ")
    fun fetchExchangeRate(@Query(SYMBOL) market: String): Single<Market>
}

const val SYMBOL = "symbol"