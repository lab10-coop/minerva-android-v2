package minerva.android.configProvider.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.migration.Migration
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import retrofit2.HttpException
import java.net.HttpURLConnection

class MinervaApiRepositoryImpl(private val api: MinervaApi) : MinervaApiRepository {

    override fun getWalletConfig(publicKey: String): Single<WalletConfigPayload> =
        api.getWalletConfig(publicKey = publicKey)
            .map { Migration.migrateIfNeeded(it) }

    override fun getWalletConfigVersion(publicKey: String): Single<Int> =
        api.getWalletConfigVersion(publicKey = publicKey)
            .map { it.version }

    override fun saveWalletConfig(publicKey: String, walletConfigPayload: WalletConfigPayload): Completable =
        api.saveWalletConfig(publicKey = publicKey, walletConfigPayload = walletConfigPayload)
            .onErrorResumeNext {
                if (it is HttpException && it.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    Completable.error(HttpBadRequestException())
                } else {
                    Completable.error(it)
                }
            }

    override fun getWalletActions(publicKey: String): Observable<WalletActionsResponse> =
        api.getWalletActions(publicKey = publicKey)

    override fun saveWalletActions(publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable =
        api.saveWalletActions(publicKey = publicKey, walletActionsConfigPayload = walletActionsConfigPayload)
}

class HttpBadRequestException : Throwable()