package minerva.android.walletmanager.walletActions

import io.reactivex.Completable
import io.reactivex.Observable
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.wallet.WalletActionClustered

interface WalletActionsRepository {
    fun getWalletActions(): Observable<List<WalletActionClustered>>
    fun saveWalletActions(walletActions: List<WalletAction>): Completable
    val isSynced: Boolean
}