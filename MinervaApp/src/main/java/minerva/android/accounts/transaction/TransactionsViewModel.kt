package minerva.android.accounts.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.AMOUNT
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.NETWORK
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.RECEIVER
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionsViewModel(
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    lateinit var transaction: Transaction

    var account: Account = Account(Int.InvalidIndex)
    var assetIndex: Int = Int.InvalidIndex

    var transactionCost: BigDecimal = BigDecimal.ZERO
    lateinit var recipients: List<Recipient>

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _saveWalletActionFailedLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val saveWalletActionFailedLiveData: LiveData<Event<Pair<String, Int>>> get() = _saveWalletActionFailedLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _transactionCostLoadingLiveData = MutableLiveData<Event<Boolean>>()
    val transactionCostLoadingLiveData: LiveData<Event<Boolean>> get() = _transactionCostLoadingLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val sendTransactionLiveData: LiveData<Event<Pair<String, Int>>> get() = _sendTransactionLiveData

    private val _transactionCompletedLiveData = MutableLiveData<Event<Any>>()
    val transactionCompletedLiveData: LiveData<Event<Any>> get() = _transactionCompletedLiveData

    private val _transactionCostLiveData = MutableLiveData<Event<TransactionCost>>()
    val transactionCostLiveData: LiveData<Event<TransactionCost>> get() = _transactionCostLiveData

    val wssUri get() = account.network.wsRpc

    private val isMainTransaction
        get() = assetIndex == Int.InvalidIndex && !account.isSafeAccount

    private val isSafeAccountMainTransaction
        get() = assetIndex == Int.InvalidIndex && account.isSafeAccount

    private val isAssetTransaction
        get() = assetIndex != Int.InvalidIndex && !account.isSafeAccount

    private val isSafeAccountAssetTransaction
        get() = assetIndex != Int.InvalidIndex && account.isSafeAccount

    fun getAccount(accountIndex: Int, assetIndex: Int) {
        transactionRepository.getAccount(accountIndex)?.let {
            account = it
            this.assetIndex = assetIndex
        }
    }

    fun loadRecipients() {
        recipients = transactionRepository.loadRecipients()
    }

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address)

    fun getTransactionCosts(to: String, amount: BigDecimal) {
        launchDisposable {
            transactionRepository.getTransactionCosts(network.short, assetIndex, account.address, to, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _transactionCostLoadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _transactionCostLoadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = {
                        transactionCost = it.cost
                        _transactionCostLiveData.value = Event(it)
                    },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }


    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        when {
            isMainTransaction -> sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            isSafeAccountMainTransaction -> sendSafeAccountMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            isAssetTransaction -> sendAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
            isSafeAccountAssetTransaction -> sendSafeAccountAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
        }
    }

    private fun sendSafeAccountAssetTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ) {
        launchDisposable {
            val ownerPrivateKey =
                account.masterOwnerAddress.let { smartContractRepository.getSafeAccountMasterOwnerPrivateKey(it) }
            transactionRepository.resolveENS(receiverKey)
                .flatMap { resolvedENS ->
                    getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                        .flatMap {
                            transaction = it
                            smartContractRepository.transferERC20Token(network.short, it, assetAddress).toSingleDefault(it)
                        }
                }
                .onErrorResumeNext { error -> SingleSource { saveTransferFailedWalletAction(error.message) } }
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

    private fun sendSafeAccountMainTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ) {
        launchDisposable {
            val ownerPrivateKey =
                account.masterOwnerAddress.let { smartContractRepository.getSafeAccountMasterOwnerPrivateKey(it) }
            transactionRepository.resolveENS(receiverKey)
                .flatMap { resolvedENS ->
                    getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                        .flatMap {
                            transaction = it
                            smartContractRepository.transferNativeCoin(network.short, it).toSingleDefault(it)
                        }
                }
                .onErrorResumeNext { error -> SingleSource { saveTransferFailedWalletAction(error.message) } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    },
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
    ): Single<Transaction> =
        Single.just(
            Transaction(
                privateKey = ownerPrivateKey,
                receiverKey = receiverKey,
                amount = amount,
                gasPrice = gasPrice,
                gasLimit = gasLimit,
                contractAddress = account.address
            )
        )

    private fun sendMainTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit)
                .flatMap {
                    transaction = it
                    transactionRepository.transferNativeCoin(network.short, account.id, it).toSingleDefault(it)
                }
                .onErrorResumeNext { error -> SingleSource { saveTransferFailedWalletAction(error.message) } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _transactionCompletedLiveData.value = Event(Any()) },
                    onError = {
                        Timber.e("Send transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    }
                )
        }
    }

    private fun sendAssetTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit, assetAddress)
                .flatMapCompletable { transactionRepository.transferERC20Token(account.network.short, it) }
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

    private val assetAddress
        get() = account.accountAssets[assetIndex].asset.address

    val cryptoBalance: BigDecimal
        get() = if (assetIndex == Int.InvalidIndex) account.cryptoBalance else account.accountAssets[assetIndex].balance

    fun getAllAvailableFunds(): String {
        if (assetIndex != Int.InvalidIndex) return account.accountAssets[assetIndex].balance.toPlainString()
        if (account.isSafeAccount) return account.cryptoBalance.toPlainString()

        val allAvailableFunds = account.cryptoBalance.minus(transactionCost)
        return if (allAvailableFunds < BigDecimal.ZERO) {
            String.EmptyBalance
        } else {
            allAvailableFunds.toPlainString()
        }
    }

    val recalculateAmount: BigDecimal
        get() = cryptoBalance.minus(transactionCost)

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        transactionRepository.calculateTransactionCost(gasPrice, gasLimit).apply { transactionCost = this }


    fun prepareCurrency() =
        if (assetIndex != Int.InvalidIndex) account.accountAssets[assetIndex].asset.nameShort else account.network.token

    private fun getAccountsWalletAction(transaction: Transaction, network: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.ACCOUNT,
            status,
            DateUtils.timestamp,
            hashMapOf(
                Pair(AMOUNT, transaction.amount.toPlainString()),
                Pair(NETWORK, network),
                Pair(RECEIVER, transaction.receiverKey)
            )
        )

    private fun saveTransferFailedWalletAction(message: String?) {
        launchDisposable {
            saveWalletAction(FAILED, transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        Timber.e(message)
                        _sendTransactionLiveData.value =
                            Event(Pair(message ?: "${transaction.amount} ${account.network.token}", FAILED))
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
        transactionRepository.resolveENS(receiverKey)
            .map { prepareTransaction(it, amount, gasPrice, gasLimit, contractAddress).apply { transaction = this } }

    private fun saveWalletAction(status: Int, transaction: Transaction): Completable =
        walletActionsRepository.saveWalletActions(listOf(getAccountsWalletAction(transaction, network.short, status)))

    private fun prepareTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        contractAddress: String = String.Empty
    ): Transaction = Transaction(account.address, account.privateKey, receiverKey, amount, gasPrice, gasLimit, contractAddress)

    val network
        get() = account.network

    val token
        get() = network.token


    fun prepareTitle() =
        if (assetIndex != Int.InvalidIndex) account.accountAssets[assetIndex].asset.name else network.token
}