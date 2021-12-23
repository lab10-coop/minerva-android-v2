package minerva.android.configProvider.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.migration.Migration
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.repository.UpdateWalletConfigInfo.Companion.NO_PENDING_UPDATES
import minerva.android.kotlinUtils.InvalidValue
import retrofit2.HttpException
import java.net.HttpURLConnection

class MinervaApiRepositoryImpl(private val api: MinervaApi) : MinervaApiRepository {

    private var updateWalletConfigInfo: UpdateWalletConfigInfo = UpdateWalletConfigInfo()

    override fun getWalletConfig(publicKey: String): Single<WalletConfigPayload> =
        api.getWalletConfig(publicKey = publicKey)
            .map { walletConfigJson -> Migration.migrateIfNeeded(walletConfigJson) }
            .onErrorResumeNext { error -> Single.error(getThrowableWhenGettingWalletConfig(error)) }

    private fun getThrowableWhenGettingWalletConfig(error: Throwable) =
        if (error is HttpException && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            HttpNotFoundException()
        } else {
            error
        }

    override fun getWalletConfigVersion(publicKey: String): Single<Int> =
        api.getWalletConfigVersion(publicKey = publicKey)
            .map { it.version }

    override fun saveWalletConfig(
        publicKey: String,
        walletConfigPayload: WalletConfigPayload
    ): Single<WalletConfigPayload> {
        val payload =
            if (isCorrectVersion(walletConfigPayload.version)) {
                walletConfigPayload.copy(_version = updateWalletConfigInfo.syncVersion.inc())
            } else walletConfigPayload
        updateWalletConfigInfo = UpdateWalletConfigInfo(updateWalletConfigInfo.pendingUpdates.inc(), payload.version)
        return saveWalletConfigCall(publicKey, payload)
    }

    private fun isCorrectVersion(version: Int): Boolean =
        updateWalletConfigInfo.pendingUpdates > NO_PENDING_UPDATES || version <= updateWalletConfigInfo.syncVersion

    private fun saveWalletConfigCall(
        publicKey: String,
        walletConfigPayload: WalletConfigPayload
    ): Single<WalletConfigPayload> =
        api.saveWalletConfig(publicKey = publicKey, walletConfigPayload = walletConfigPayload)
            .toSingleDefault(walletConfigPayload)
            .doOnSuccess {
                stopUpdateWalletConfigInfo()
            }
            .onErrorResumeNext { error ->
                stopUpdateWalletConfigInfo()
                Single.error(getThrowableWhenSavingWalletConfig(error))
            }
            .subscribeOn(Schedulers.io())

    private fun stopUpdateWalletConfigInfo() {
        updateWalletConfigInfo = updateWalletConfigInfo.copy(pendingUpdates = updateWalletConfigInfo.pendingUpdates.dec())
    }

    private fun getThrowableWhenSavingWalletConfig(error: Throwable) =
        if (error is HttpException && error.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
            HttpBadRequestException()
        } else {
            error
        }

    override fun getWalletActions(publicKey: String): Observable<WalletActionsResponse> =
        api.getWalletActions(publicKey = publicKey)

    override fun saveWalletActions(
        publicKey: String,
        walletActionsConfigPayload: WalletActionsConfigPayload
    ): Completable = api.saveWalletActions(publicKey = publicKey, walletActionsConfigPayload = walletActionsConfigPayload)
}

class HttpBadRequestException : Throwable()
class HttpNotFoundException : Throwable()

data class UpdateWalletConfigInfo(
    val pendingUpdates: Int = NO_PENDING_UPDATES,
    val syncVersion: Int = Int.InvalidValue
) {
    companion object {
        const val NO_PENDING_UPDATES = 0
    }
}