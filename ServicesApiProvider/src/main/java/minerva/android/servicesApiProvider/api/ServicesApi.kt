package minerva.android.servicesApiProvider.api

import io.reactivex.Single
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.TokenPayload
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ServicesApi {
    @POST
    fun painlessLogin(@Header(CONTENT_TYPE) content: String = APPLICATION_JSON, @Url url: String, @Body tokenPayload: TokenPayload): Single<LoginResponse>

    companion object {
        const val CONTENT_TYPE = "Content-typex"
        const val APPLICATION_JSON = "application/json"
    }
}