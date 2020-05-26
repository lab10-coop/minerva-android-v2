package minerva.android.walletmanager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered

interface WalletActionsRepository {
    fun getWalletActions(masterSeed: MasterSeed): Observable<List<WalletActionClustered>>
    fun saveWalletActions(walletAction: WalletAction, masterSeed: MasterSeed): Completable
}