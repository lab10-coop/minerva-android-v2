package minerva.android.configProvider.api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import retrofit2.http.*

interface MinervaApi {
    @GET("backups/{$PUBLIC_KEY}")
    fun getWalletConfig(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String
    ): Single<WalletConfigResponse>

    @PUT("backups/{$PUBLIC_KEY}")
    fun saveWalletConfig(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String, @Body walletConfigPayload: WalletConfigPayload
    ): Completable

    @GET("activities/{$PUBLIC_KEY}")
    fun getWalletActions(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String
    ): Observable<WalletActionsResponse>

    @PUT("activities/{$PUBLIC_KEY}")
    fun saveWalletActions(
        @Header(CONTENT_TYPE) content: String = APPLICATION_JSON,
        @Path(PUBLIC_KEY) publicKey: String,
        @Body walletActionsConfigPayload: WalletActionsConfigPayload
    ): Completable

    companion object {
        const val PUBLIC_KEY = "publicKey"
        const val CONTENT_TYPE = "Content-type"
        const val APPLICATION_JSON = "application/json"
    }
}