package minerva.android.apiProvider.api

import io.reactivex.Single
import minerva.android.apiProvider.api.ServicesApi.Companion.APPLICATION_JSON
import minerva.android.apiProvider.api.ServicesApi.Companion.CONTENT_TYPE
import minerva.android.apiProvider.model.*
import minerva.android.apiProvider.model.CommitElement
import minerva.android.apiProvider.model.GasPrices
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.TokenIconDetails
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface CryptoApi {

    @GET("simple/price")
    fun getMarkets(@Query(IDS) coinIds: String, @Query(VS_CURRENCIES) currency: String): Single<Markets>

    @GET
    fun getGasPrice(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Url url: String
    ): Single<GasPrices>

    @GET
    fun getTokenRawData(@Url url: String) : Single<List<TokenIconDetails>>

    @GET
    fun getTokenLastCommitRawData(@Url url: String) : Single<List<CommitElement>>

    @GET
    fun getTokenBalance(@Url url: String) : Single<TokenBalanceResponse>

    @GET
    fun getTokenTx(@Url url: String) : Single<TokenTxResponse>

    companion object {
        private const val IDS = "ids"
        private const val VS_CURRENCIES = "vs_currencies"
    }
}