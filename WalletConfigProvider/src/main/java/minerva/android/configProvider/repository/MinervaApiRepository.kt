package minerva.android.configProvider.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload

interface MinervaApiRepository {
    fun getWalletConfig(publicKey: String): Single<WalletConfigPayload>
    fun getWalletConfigVersion(publicKey: String): Single<Int>
    fun saveWalletConfig(publicKey: String, walletConfigPayload: WalletConfigPayload): Completable
    fun getWalletActions(publicKey: String): Observable<WalletActionsResponse>
    fun saveWalletActions(publicKey: String, walletActionsConfigPayload: WalletActionsConfigPayload): Completable
}