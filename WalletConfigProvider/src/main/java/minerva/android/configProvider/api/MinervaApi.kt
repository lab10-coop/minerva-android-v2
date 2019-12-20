package minerva.android.configProvider.api

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import retrofit2.http.*

interface MinervaApi {
    @GET("{$PUBLIC_KEY}")
    fun getWalletConfig(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String
    ): Single<WalletConfigResponse>

    @PUT("{$PUBLIC_KEY}")
    fun saveWalletConfig(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String, @Body walletConfigPayload: WalletConfigPayload
    ): Completable

    companion object {
        const val PUBLIC_KEY = "publicKey"
        const val CONTENT_TYPE = "Content-type"
        const val APPLICATION_JSON = "application/json"
    }
}