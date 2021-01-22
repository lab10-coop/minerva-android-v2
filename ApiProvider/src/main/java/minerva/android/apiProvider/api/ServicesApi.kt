package minerva.android.apiProvider.api

import io.reactivex.Single
import minerva.android.apiProvider.model.LoginResponse
import minerva.android.apiProvider.model.AccessTokenPayload
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ServicesApi {
    @POST
    fun painlessLogin(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Url url: String,
        @Body accessTokenPayload: AccessTokenPayload
    ): Single<LoginResponse>

    companion object {
        const val CONTENT_TYPE = "Content-type"
        const val APPLICATION_JSON = "application/json"
    }
}