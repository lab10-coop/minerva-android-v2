package minerva.android.walletmanager.manager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered

interface WalletActionsRepository {
    fun getWalletActions(masterKey: MasterKey): Observable<List<WalletActionClustered>>
    fun saveWalletActions(walletAction: WalletAction, masterKey: MasterKey): Completable
}