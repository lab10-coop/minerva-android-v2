package minerva.android.walletmanager.manager.order

import io.reactivex.Completable
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.MinervaPrimitive

interface OrderManager : Manager {
    fun updateList(type: Int, newOrderList: List<MinervaPrimitive>): Completable
    fun prepareList(type: Int): List<MinervaPrimitive>
    fun isOrderAvailable(type: Int): Boolean
    val areMainNetsEnabled: Boolean
}