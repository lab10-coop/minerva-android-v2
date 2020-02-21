package minerva.android.values.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.TransactionCost
import minerva.android.walletmanager.model.Value
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionsViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    var value: Value = Value(Int.InvalidIndex)
    var assetIndex: Int = Int.InvalidIndex
    var transactionCost: BigDecimal = BigDecimal.ZERO

    private val _getValueLiveData = MutableLiveData<Event<Value>>()
    val getValueLiveData: LiveData<Event<Value>> get() = _getValueLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<String>>()
    val sendTransactionLiveData: LiveData<Event<String>> get() = _sendTransactionLiveData

    private val _errorTransactionLiveData = MutableLiveData<Event<String?>>()
    val errorTransactionLiveData: LiveData<Event<String?>> get() = _errorTransactionLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _transactionCostLiveData = MutableLiveData<Event<TransactionCost>>()
    val transactionCostLiveData: MutableLiveData<Event<TransactionCost>> get() = _transactionCostLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun getValue(valueIndex: Int, assetIndex: Int) {
        walletManager.walletConfigLiveData.value?.values?.forEach {
            if (it.index == valueIndex) {
                value = it
                this.assetIndex = assetIndex
                _getValueLiveData.value = Event(it)
            }
        }
    }

    fun getTransactionCosts() {
        launchDisposable {
            walletManager.getTransactionCosts(network, assetIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        transactionCost = it.cost
                        _transactionCostLiveData.value = Event(it)
                    },
                    onError = {
                        Timber.e("Transaction cost error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        if (assetIndex == Int.InvalidIndex) sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
        else sendAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
    }

    private fun sendMainTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            walletManager.sendTransaction(network, prepareTransaction(receiverKey, amount, gasPrice, gasLimit))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value = Event("$amount ${value.network}")
                    },
                    onError = {
                        Timber.e("Send transaction error: ${it.message}")
                        _errorTransactionLiveData.value = Event(it.message)
                    }
                )
        }
    }

    private fun sendAssetTransaction(toAddress: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            walletManager.transferERC20Token(
                value.network,
                prepareTransaction(toAddress, amount, gasPrice, gasLimit, value.assets[assetIndex].address)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _sendTransactionLiveData.value = Event("$amount ${value.network}") },
                    onError = { _errorTransactionLiveData.value = Event(it.message) }
                )
        }
    }

    private fun prepareTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        contractAddress: String = String.Empty
    ): Transaction {
        return Transaction(
            value.address,
            value.privateKey,
            receiverKey,
            amount,
            gasPrice,
            gasLimit,
            contractAddress
        )
    }

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): String =
        walletManager.calculateTransactionCost(gasPrice, gasLimit).toPlainString()

    fun getBalance(): BigDecimal = if(assetIndex == Int.InvalidIndex) value.balance else value.assets[assetIndex].balance

    fun getAllAvailableFunds(): String {
        value.balance.minus(transactionCost).apply {
            return if (this < BigDecimal.ZERO) {
                String.EmptyBalance
            } else {
                this.toPlainString()
            }
        }
    }

    val network
        get() = value.network
}