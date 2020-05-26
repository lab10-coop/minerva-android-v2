package minerva.android.walletmanager.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletConfig

interface WalletConfigRepository {
    fun loadWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig>
    fun getWalletConfig(masterSeed: MasterSeed): Single<WalletConfigResponse>
    fun updateWalletConfig(masterSeed: MasterSeed, walletConfigPayload: WalletConfigPayload): Completable
    fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload)
    fun createWalletConfig(masterSeed: MasterSeed): Completable
    fun createDefaultWalletConfig(): WalletConfigPayload
}