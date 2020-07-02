package minerva.android.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.WalletConfig

class EditOrderViewModel(
    private val orderManager: OrderManager
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = orderManager.walletConfigLiveData

    private val _saveNewOrderLiveData = MutableLiveData<Event<Unit>>()
    val saveNewOrderLiveData: LiveData<Event<Unit>> get() = _saveNewOrderLiveData

    fun prepareList(type: Int): List<Account> = orderManager.prepareList(type)

    fun saveChanges(type: Int, newOrderList: List<Account>) {
        launchDisposable {
            orderManager.updateList(type, newOrderList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _saveNewOrderLiveData.value = Event(Unit) },
                    onError = { _saveNewOrderLiveData.value = Event(Unit) }
                )
        }
    }
}