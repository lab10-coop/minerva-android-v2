package minerva.android.servicesApiProvider.api

import io.reactivex.Single
import minerva.android.servicesApiProvider.model.GasPrice
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.TokenPayload
import retrofit2.http.*

interface ServicesApi {
    @POST
    fun painlessLogin(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Url url: String,
        @Body tokenPayload: TokenPayload
    ): Single<LoginResponse>

    @GET
    fun getGasPrice(@Header(CONTENT_TYPE) content: String = APPLICATION_JSON, @Url url: String): Single<GasPrice>

    companion object {
        const val CONTENT_TYPE = "Content-type"
        const val APPLICATION_JSON = "application/json"
    }
}