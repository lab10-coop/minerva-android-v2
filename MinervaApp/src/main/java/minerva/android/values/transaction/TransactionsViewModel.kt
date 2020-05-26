package minerva.android.values.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.AMOUNT
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.NETWORK
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.RECEIVER
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionsViewModel(
    private val walletManager: WalletManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractManager: SmartContractManager
) : BaseViewModel() {

    lateinit var transaction: Transaction

    internal var value: Value = Value(Int.InvalidIndex)
    internal var assetIndex: Int = Int.InvalidIndex
    var transactionCost: BigDecimal = BigDecimal.ZERO
    lateinit var recipients: List<Recipient>

    private val _getValueLiveData = MutableLiveData<Event<Value>>()
    val getValueLiveData: LiveData<Event<Value>> get() = _getValueLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _saveWalletActionFailedLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val saveWalletActionFailedLiveData: LiveData<Event<Pair<String, Int>>> get() = _saveWalletActionFailedLiveData

    private val _transactionCostLiveData = MutableLiveData<Event<TransactionCost>>()
    val transactionCostLiveData: MutableLiveData<Event<TransactionCost>> get() = _transactionCostLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val sendTransactionLiveData: LiveData<Event<Pair<String, Int>>> get() = _sendTransactionLiveData

    fun getValue(valueIndex: Int, assetIndex: Int) {
        walletManager.walletConfigLiveData.value?.values?.forEach {
            if (it.index == valueIndex) {
                value = it
                this.assetIndex = assetIndex
                _getValueLiveData.value = Event(it)
            }
        }
    }

    fun loadRecipients() {
        recipients = walletManager.loadRecipients()
    }

    fun getTransactionCosts() {
        walletManager.getTransferCosts(network, assetIndex).let {
            transactionCost = it.cost
            _transactionCostLiveData.value = Event(it)
        }
    }

    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        when {
            assetIndex == Int.InvalidIndex && !value.isSafeAccount -> sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex == Int.InvalidIndex && value.isSafeAccount -> sendSafeAccountMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex != Int.InvalidIndex && !value.isSafeAccount -> sendAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex != Int.InvalidIndex && value.isSafeAccount -> sendSafeAccountAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
        }
    }

    private fun sendSafeAccountAssetTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            val ownerPrivateKey = value.masterOwnerAddress.let { walletManager.getSafeAccountMasterOwnerPrivateKey(it) }
            walletManager.resolveENS(receiverKey).flatMap { resolvedENS ->
                getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                    .flatMap {
                        transaction = it
                        smartContractManager.transferERC20Token(network, it, value.assets[assetIndex].address).toSingleDefault(it)
                    }
            }
                .onErrorResumeNext { SingleSource { saveTransferFailedWalletAction() } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT)) },
                    onError = {
                        Timber.e("Send safe account transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    }
                )
        }

    }

    private fun sendSafeAccountMainTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            val ownerPrivateKey = value.masterOwnerAddress.let { walletManager.getSafeAccountMasterOwnerPrivateKey(it) }
            walletManager.resolveENS(receiverKey).flatMap { resolvedENS ->
                getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                    .flatMap {
                        transaction = it
                        smartContractManager.transferNativeCoin(network, it).toSingleDefault(it)
                    }
            }
                .onErrorResumeNext { SingleSource { saveTransferFailedWalletAction() } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT)) },
                    onError = {
                        Timber.e("Send safe account transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    }
                )
        }
    }

    private fun getTransactionForSafeAccount(
        ownerPrivateKey: String,
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ): Single<Transaction> {
        return Single.just(
            Transaction(
                privateKey = ownerPrivateKey,
                receiverKey = receiverKey,
                amount = amount,
                gasPrice = gasPrice,
                gasLimit = gasLimit,
                contractAddress = value.address
            )
        )
    }

    private fun sendMainTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit)
                .flatMap {
                    walletManager.transferNativeCoin(network, it).toSingleDefault(it)
                }
                .onErrorResumeNext { SingleSource { saveTransferFailedWalletAction() } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT)) },
                    onError = {
                        Timber.e("Send transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    }
                )
        }
    }

    private fun sendAssetTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit, value.assets[assetIndex].address)
                .flatMapCompletable {
                    walletManager.transferERC20Token(value.network, it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT)) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): String {
        walletManager.calculateTransactionCost(gasPrice, gasLimit).apply {
            transactionCost = this
            return transactionCost.toPlainString()
        }
    }

    fun getBalance(): BigDecimal = if (assetIndex == Int.InvalidIndex) value.balance else value.assets[assetIndex].balance

    fun getAllAvailableFunds(): String {
        if (assetIndex != Int.InvalidIndex) return value.assets[assetIndex].balance.toPlainString()
        if (value.isSafeAccount) return value.balance.toPlainString()

        value.balance.minus(transactionCost).apply {
            return if (this < BigDecimal.ZERO) {
                String.EmptyBalance
            } else {
                this.toPlainString()
            }
        }
    }

    fun prepareCurrency() = if (assetIndex != Int.InvalidIndex) value.assets[assetIndex].nameShort else value.network

    private fun getValuesWalletAction(transaction: Transaction, network: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.VALUE,
            status,
            DateUtils.timestamp,
            hashMapOf(
                Pair(AMOUNT, transaction.amount.toPlainString()),
                Pair(NETWORK, network),
                Pair(RECEIVER, transaction.receiverKey)
            )
        )

    private fun saveTransferFailedWalletAction() {
        launchDisposable {
            saveWalletAction(FAILED, transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value = Event(Pair("${transaction.amount} ${value.network}", FAILED))
                    },
                    onError = {
                        Timber.e("Save wallet action error $it")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun resolveENS(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        contractAddress: String = String.Empty
    ): Single<Transaction> =
        walletManager.resolveENS(receiverKey)
            .map { prepareTransaction(it, amount, gasPrice, gasLimit, contractAddress).apply { transaction = this } }

    private fun saveWalletAction(status: Int, transaction: Transaction): Completable =
        walletActionsRepository.saveWalletActions(
            getValuesWalletAction(transaction, network, status),
            walletManager.masterSeed
        )

    private fun prepareTransaction(
        receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger, contractAddress: String = String.Empty
    ): Transaction = Transaction(value.address, value.privateKey, receiverKey, amount, gasPrice, gasLimit, contractAddress)

    val network
        get() = value.network
}