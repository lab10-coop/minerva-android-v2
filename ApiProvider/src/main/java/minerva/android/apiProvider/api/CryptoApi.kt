package minerva.android.apiProvider.api

import io.reactivex.Single
import minerva.android.apiProvider.model.GasPrice
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
        @Header(ServicesApi.CONTENT_TYPE) content: String = ServicesApi.APPLICATION_JSON,
        @Url url: String
    ): Single<GasPrice>

    @GET
    fun getTokenRawData(
        @Header(ServicesApi.CONTENT_TYPE) content: String = ServicesApi.APPLICATION_JSON,
        @Url url: String
    ) : Single<List<TokenIconDetails>>

    companion object {
        private const val IDS = "ids"
        private const val VS_CURRENCIES = "vs_currencies"
    }
}