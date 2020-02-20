package minerva.android.walletmanager.manager.wallet.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig

interface WalletConfigRepository {
    fun loadWalletConfig(masterKey: MasterKey): Observable<WalletConfig>
    fun getWalletConfig(masterKey: MasterKey): Single<WalletConfigResponse>
    fun updateWalletConfig(masterKey: MasterKey, walletConfigPayload: WalletConfigPayload): Completable
    fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload)
    fun createWalletConfig(masterKey: MasterKey): Completable
    fun createDefaultWalletConfig(): WalletConfigPayload
}