package minerva.android.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.wallet.WalletConfig

class EditOrderViewModel(
    private val orderManager: OrderManager
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = orderManager.walletConfigLiveData

    private val _saveNewOrderLiveData = MutableLiveData<Event<Unit>>()
    val saveNewOrderLiveData: LiveData<Event<Unit>> get() = _saveNewOrderLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    val areMainNetsEnabled
        get() = orderManager.areMainNetsEnabled

    fun prepareList(type: Int): List<MinervaPrimitive> = orderManager.prepareList(type)

    fun saveChanges(type: Int, newOrderList: List<MinervaPrimitive>) {
        launchDisposable {
            orderManager.updateList(type, newOrderList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _saveNewOrderLiveData.value = Event(Unit) },
                    onError = { _errorMutableLiveData.value = Event(it) }
                )
        }
    }
}