package minerva.android.walletmanager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered

interface WalletActionsRepository {
    fun getWalletActions(): Observable<List<WalletActionClustered>>
    fun saveWalletActions(walletAction: WalletAction): Completable
}