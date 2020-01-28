package minerva.android.values.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Value
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionsViewModel(private val walletManager: WalletManager) : ViewModel() {

    private var disposable: Disposable? = null
    var value: Value = Value(Int.InvalidIndex)

    private val _getValueLiveData = MutableLiveData<Event<Value>>()
    val getValueLiveData: LiveData<Event<Value>> get() = _getValueLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<String>>()
    val sendTransactionLiveData: LiveData<Event<String>> get() = _sendTransactionLiveData

    private val _errorTransactionLiveData = MutableLiveData<Event<Throwable>>()
    val errorTransactionLiveData: LiveData<Event<Throwable>> get() = _errorTransactionLiveData

    private val _errorTransactionCostLiveData = MutableLiveData<Event<Throwable>>()
    val errorTransactionCostLiveData: LiveData<Event<Throwable>> get() = _errorTransactionCostLiveData

    private val _transactionCostLiveData = MutableLiveData<Event<Triple<BigDecimal, BigInteger, BigDecimal>>>()
    val transactionCostLiveData: MutableLiveData<Event<Triple<BigDecimal, BigInteger, BigDecimal>>> get() = _transactionCostLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun getValue(index: Int) {
        walletManager.walletConfigLiveData.value?.values?.forEach {
            if (it.index == index) {
                value = it
                _getValueLiveData.value = Event(it)
            }
        }
    }

    fun getTransactionCosts() {
        disposable = walletManager.getTransactionCosts()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    _transactionCostLiveData.value = Event(it)
                },
                onError = {
                    Timber.e("Transaction cost error: ${it.message}")
                    _errorTransactionCostLiveData.value = Event(it)
                }
            )
    }

    fun sendTransaction(receiverKey: String, amount: String, gasPrice: BigDecimal, gasLimit: BigInteger) {
        _loadingLiveData.value = Event(true)
        disposable = walletManager.sendTransaction(value.privateKey, receiverKey, amount, gasPrice, gasLimit)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
            .subscribeBy(
                onSuccess = {
                    _sendTransactionLiveData.value = Event("$amount ${value.network}")
                },
                onError = {
                    Timber.e("Send transaction error: ${it.message}")
                    _errorTransactionLiveData.value = Event(it)
                }
            )
    }

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): String =
        walletManager.calculateTransactionCost(gasPrice, gasLimit).toPlainString()

    val network
        get() = value.network
}