package minerva.android.walletmanager.manager.order

import io.reactivex.Completable
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Account

interface OrderManager : Manager {
    fun updateList(type: Int, newOrderList: List<Account>): Completable
    fun prepareList(type: Int): List<Account>
}