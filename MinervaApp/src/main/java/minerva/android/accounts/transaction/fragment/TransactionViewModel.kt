package minerva.android.accounts.transaction.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.model.TokenSpinnerElement
import minerva.android.base.BaseViewModel
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.AMOUNT
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.RECEIVER
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.TOKEN
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.widget.repository.getMainTokenIconRes
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionViewModel(
    private val walletActionsRepository: WalletActionsRepository,
    private val smartContractRepository: SmartContractRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    lateinit var transaction: Transaction

    var accountIndex = Int.InvalidIndex
    var tokenIndex: Int = Int.InvalidIndex
    var account: Account = Account(Int.InvalidIndex)

    var transactionCost: BigDecimal = BigDecimal.ZERO
    lateinit var recipients: List<Recipient>

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _overrideTxCostLiveData = MutableLiveData<Event<Any>>()
    val overrideTxCostLiveData: LiveData<Event<Any>> get() = _overrideTxCostLiveData

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

    val wssUri
        get() = account.network.wsRpc

    val network
        get() = account.network

    val token
        get() = network.token

    //TODO add logos for tokens
    val tokensList: List<TokenSpinnerElement>
        get() = mutableListOf<TokenSpinnerElement>().apply {
            add(TokenSpinnerElement(account.network.token, getMainTokenIconRes(account.network.short)))
            account.accountTokens.forEach {
                add(TokenSpinnerElement(it.token.name))
            }
        }

    private val isMainTransaction
        get() = tokenIndex == Int.InvalidIndex && !account.isSafeAccount

    private val isSafeAccountMainTransaction
        get() = tokenIndex == Int.InvalidIndex && account.isSafeAccount

    private val isTokenTransaction
        get() = tokenIndex != Int.InvalidIndex && !account.isSafeAccount

    private val isSafeAccountTokenTransaction
        get() = tokenIndex != Int.InvalidIndex && account.isSafeAccount

    fun getAccount(accountIndex: Int, tokenIndex: Int) {
        transactionRepository.getAccount(accountIndex)?.let {
            account = it
            this.tokenIndex = tokenIndex
        }
    }

    fun loadRecipients() {
        recipients = transactionRepository.loadRecipients()
    }

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address)

    fun getTransactionCosts(to: String, amount: BigDecimal) {
        launchDisposable {
            transactionRepository.getTransactionCosts(network.short, tokenIndex, account.address, to, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    _transactionCostLoadingLiveData.value = Event(true)
                    if (Validator.isEnsName(to)) _overrideTxCostLiveData.value = Event(Any())
                }
                .doOnEvent { _, _ -> _transactionCostLoadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = {
                        transactionCost = it.cost
                        _transactionCostLiveData.value = Event(it)
                    },
                    onError = {
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }


    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        when {
            isMainTransaction -> sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            isSafeAccountMainTransaction -> sendSafeAccountMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            isTokenTransaction -> sendTokenTransaction(receiverKey, amount, gasPrice, gasLimit)
            isSafeAccountTokenTransaction -> sendSafeAccountTokenTransaction(receiverKey, amount, gasPrice, gasLimit)
        }
    }

    private fun sendSafeAccountTokenTransaction(
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
                            smartContractRepository.transferERC20Token(network.short, it, tokenAddress).toSingleDefault(it)
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

    private fun sendTokenTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit, tokenAddress)
                .flatMapCompletable { transactionRepository.transferERC20Token(account.network.short, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value = Event(Pair("$amount ${prepareCurrency()}", SENT))
                                 },
                    onError = {
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private val tokenAddress
        get() = account.accountTokens[tokenIndex].token.address

    val cryptoBalance: BigDecimal
        get() = if (tokenIndex == Int.InvalidIndex) account.cryptoBalance else account.accountTokens[tokenIndex].balance

    fun getAllAvailableFunds(): String {
        if (tokenIndex != Int.InvalidIndex) return account.accountTokens[tokenIndex].balance.toPlainString()
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
        if (tokenIndex != Int.InvalidIndex) account.accountTokens[tokenIndex].token.symbol else token

    private fun getAccountsWalletAction(transaction: Transaction, token: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.ACCOUNT,
            status,
            DateUtils.timestamp,
            hashMapOf(
                Pair(AMOUNT, transaction.amount.toPlainString()),
                Pair(TOKEN, token),
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
                            Event(Pair(message ?: "${transaction.amount} $token", FAILED))
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
        walletActionsRepository.saveWalletActions(listOf(getAccountsWalletAction(transaction, network.token, status)))

    private fun prepareTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        contractAddress: String = String.Empty
    ): Transaction = Transaction(account.address, account.privateKey, receiverKey, amount, gasPrice, gasLimit, contractAddress)
}