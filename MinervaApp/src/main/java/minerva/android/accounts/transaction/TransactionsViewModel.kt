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
import minerva.android.walletmanager.utils.DateUtils
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

    private val _transactionCostLiveData = MutableLiveData<Event<TransactionCost>>()
    val transactionCostLiveData: MutableLiveData<Event<TransactionCost>> get() = _transactionCostLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val sendTransactionLiveData: LiveData<Event<Pair<String, Int>>> get() = _sendTransactionLiveData

    fun getAccount(accountIndex: Int, assetIndex: Int) {
        transactionRepository.getAccount(accountIndex, assetIndex)?.let {
            account = it
            this.assetIndex = assetIndex
        }
    }

    fun loadRecipients() {
        recipients = transactionRepository.loadRecipients()
    }

    fun getTransactionCosts() {
        transactionRepository.getTransferCosts(network, assetIndex).let {
            transactionCost = it.cost
            _transactionCostLiveData.value = Event(it)
        }
    }

    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        when {
            assetIndex == Int.InvalidIndex && !account.isSafeAccount -> sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex == Int.InvalidIndex && account.isSafeAccount -> sendSafeAccountMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex != Int.InvalidIndex && !account.isSafeAccount -> sendAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
            assetIndex != Int.InvalidIndex && account.isSafeAccount -> sendSafeAccountAssetTransaction(receiverKey, amount, gasPrice, gasLimit)
        }
    }

    private fun sendSafeAccountAssetTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            val ownerPrivateKey = account.masterOwnerAddress.let { smartContractRepository.getSafeAccountMasterOwnerPrivateKey(it) }
            transactionRepository.resolveENS(receiverKey)
                .flatMap { resolvedENS ->
                    getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                        .flatMap {
                            transaction = it
                            smartContractRepository.transferERC20Token(network, it, account.assets[assetIndex].address).toSingleDefault(it)
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
            val ownerPrivateKey = account.masterOwnerAddress.let { smartContractRepository.getSafeAccountMasterOwnerPrivateKey(it) }
            transactionRepository.resolveENS(receiverKey).flatMap { resolvedENS ->
                getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                    .flatMap {
                        transaction = it
                        smartContractRepository.transferNativeCoin(network, it).toSingleDefault(it)
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
                contractAddress = account.address
            )
        )
    }

    private fun sendMainTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit)
                .flatMap {
                    transaction = it
                    transactionRepository.transferNativeCoin(network, it)
                }
                .onErrorResumeNext { SingleSource { saveTransferFailedWalletAction() } }
                .flatMap { saveWalletAction(SENT, transaction).toSingleDefault(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = {
                        transactionRepository.currentTransactionHash(it)
                        _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    },
                    onError = {
                        Timber.e("Send transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                    }
                )
        }
    }

    private fun sendAssetTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit, account.assets[assetIndex].address)
                .flatMapCompletable {
                    transactionRepository.transferERC20Token(account.network, it)
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
        transactionRepository.calculateTransactionCost(gasPrice, gasLimit).apply {
            transactionCost = this
            return transactionCost.toPlainString()
        }
    }

    fun getBalance(): BigDecimal = if (assetIndex == Int.InvalidIndex) account.cryptoBalance else account.assets[assetIndex].balance

    fun getAllAvailableFunds(): String {
        if (assetIndex != Int.InvalidIndex) return account.assets[assetIndex].balance.toPlainString()
        if (account.isSafeAccount) return account.cryptoBalance.toPlainString()

        account.cryptoBalance.minus(transactionCost).apply {
            return if (this < BigDecimal.ZERO) {
                String.EmptyBalance
            } else {
                this.toPlainString()
            }
        }
    }

    fun prepareCurrency() = if (assetIndex != Int.InvalidIndex) account.assets[assetIndex].nameShort else Network.fromString(account.network).token

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

    private fun saveTransferFailedWalletAction() {
        launchDisposable {
            saveWalletAction(FAILED, transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value = Event(Pair("${transaction.amount} ${account.network}", FAILED))
                    },
                    onError = {
                        Timber.e("Save wallet action error $it")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun resolveENS(
        receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger, contractAddress: String = String.Empty
    ): Single<Transaction> =
        transactionRepository.resolveENS(receiverKey)
            .map { prepareTransaction(it, amount, gasPrice, gasLimit, contractAddress).apply { transaction = this } }

    private fun saveWalletAction(status: Int, transaction: Transaction): Completable =
        walletActionsRepository.saveWalletActions(getAccountsWalletAction(transaction, network, status))

    private fun prepareTransaction(
        receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger, contractAddress: String = String.Empty
    ): Transaction = Transaction(account.address, account.privateKey, receiverKey, amount, gasPrice, gasLimit, contractAddress)

    val network
        get() = account.network

    fun preparePrefixAddress(prefixAddress: String, prefix: String): String =
        prefixAddress.removePrefix(prefix).replace(META_ADDRESS_SEPARATOR, String.Empty)

    fun prepareTitle() =
        if (assetIndex != Int.InvalidIndex) account.assets[assetIndex].name else Network.fromString(account.network).token

    fun isCorrectNetwork(prefixAddress: String) = account.name.contains(prefixAddress, true)

    companion object {
        const val META_ADDRESS_SEPARATOR = ":"
    }
}