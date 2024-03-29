package minerva.android.accounts.transaction.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.extension.validator.Validator
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.EmptyBalance
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.dapps.model.Dapp
import minerva.android.walletmanager.model.dapps.DappUIDetails
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.AMOUNT
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.RECEIVER
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.TOKEN
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.model.token.NativeTokenWithBalances
import minerva.android.walletmanager.model.token.TokenWithBalances
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.repository.dapps.DappsRepository
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.utils.MarketUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.widget.repository.getMainTokenIconRes
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionViewModel(
    private val walletActionsRepository: WalletActionsRepository,
    private val safeAccountRepository: SafeAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val dappsRepository: DappsRepository
) : BaseViewModel() {
    lateinit var transaction: Transaction
    val wssUri get() = account.network.wsRpc
    val network get() = account.network
    val token get() = network.token
    val spinnerPosition get() = account.getTokenIndex(tokenAddress) + ONE_ELEMENT
    var accountIndex = Int.InvalidIndex
    var tokenAddress: String = String.Empty
    var account: Account = Account(Int.InvalidIndex)
    var transactionCost: BigDecimal = BigDecimal.ZERO
    lateinit var recipients: List<Recipient>
    val fiatSymbol: String = transactionRepository.getFiatSymbol()
    val isTokenTransaction get() = tokenAddress != String.Empty && !account.isSafeAccount
    val isSafeAccountTokenTransaction get() = tokenAddress != String.Empty && account.isSafeAccount
    var isCoinBalanceError: Boolean = false
    var isTokenBalanceError: Boolean = false
    private var fiatRate: Double = Double.InvalidValue
    private var coinFiatRate: Double = Double.InvalidValue
    private val isMainTransaction get() = tokenAddress == String.Empty && !account.isSafeAccount
    private val isSafeAccountMainTransaction get() = tokenAddress == String.Empty && account.isSafeAccount

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

    private val _sponsoredDappLiveData = MutableLiveData<Event<Dapp>>()
    val sponsoredDappLiveData: LiveData<Event<Dapp>> get() = _sponsoredDappLiveData

    val tokensList: List<TokenWithBalances>
        get() = mutableListOf<TokenWithBalances>().apply {
            with(account.network) {
                add(
                    NativeTokenWithBalances(
                        NativeToken(chainId, account.name, token, getMainTokenIconRes(chainId)),
                        account.cryptoBalance,
                        account.fiatBalance
                    )
                )
                account.accountTokens
                    .filter { token -> token.token.type.isERC20() }
                    .sortedWith(
                        compareBy(
                            { it.token.logoURI.isNullOrEmpty() },
                            { it.token.symbol } )
                    )
                    .forEach { add(it) }
            }
        }

    private val tokenDecimals: Int
        get() = when (tokenAddress) {
            String.Empty -> Int.InvalidValue
            else -> account.getToken(tokenAddress).token.decimals.toInt()
        }

    val cryptoBalance: BigDecimal
        get() = if (tokenAddress == String.Empty) account.cryptoBalance else account.getToken(tokenAddress).currentBalance

    fun getAccount(accountIndex: Int, tokenAddress: String) {
        transactionRepository.getAccount(accountIndex)?.let {
            account = it
            this.tokenAddress = tokenAddress
            coinFiatRate = account.coinRate
            getSponsoredDapp()
        }
    }

    fun updateTokenAddress(index: Int) {
        tokenAddress = if (index == Int.InvalidIndex || index == INDEX_OF_NATIVE_COIN) String.Empty
        else (tokensList[index] as? AccountToken)?.token?.address ?: String.Empty
    }

    fun loadRecipients() {
        recipients = transactionRepository.loadRecipients()
    }

    private fun getSponsoredDapp() {
        launchDisposable {
            dappsRepository.getDappForChainId(network.chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.mapToDapp() }
                .subscribeBy { dapp ->
                    _sponsoredDappLiveData.value = Event(dapp)
                }
        }
    }

    private fun DappUIDetails.mapToDapp(): Dapp =
        Dapp(
            shortName, longName, subtitle, buttonColor,
            iconLink, connectLink, isSponsored, sponsoredOrder
        )

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address, network.chainId)

    fun toRecipientChecksum(address: String): String =
        transactionRepository.toRecipientChecksum(address, network.chainId)

    fun getTransactionCosts(to: String, amount: BigDecimal) {
        launchDisposable {
            transactionRepository.getTransactionCosts(getTxCostPayload(to, amount))
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

    fun isAuthenticationEnabled(): Boolean = transactionRepository.isProtectTransactionEnabled()

    private fun getTxCostPayload(to: String, amount: BigDecimal): TxCostPayload =
        TxCostPayload(
            transferType,
            account.address,
            to,
            amount,
            chainId = account.network.chainId,
            tokenDecimals = tokenDecimals,
            contractAddress = tokenAddress
        )

    private val transferType: TransferType
        get() = when {
            isMainTransaction -> TransferType.COIN_TRANSFER
            isTokenTransaction -> TransferType.TOKEN_TRANSFER
            isSafeAccountMainTransaction -> TransferType.SAFE_ACCOUNT_COIN_TRANSFER
            isSafeAccountTokenTransaction -> TransferType.SAFE_ACCOUNT_TOKEN_TRANSFER
            else -> TransferType.UNKNOWN
        }

    fun sendTransaction(receiverKey: String, amount: BigDecimal, gasPrice: BigDecimal, gasLimit: BigInteger) {
        when (transferType) {
            TransferType.COIN_TRANSFER -> sendMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            TransferType.SAFE_ACCOUNT_COIN_TRANSFER ->
                sendSafeAccountMainTransaction(receiverKey, amount, gasPrice, gasLimit)
            TransferType.TOKEN_TRANSFER -> sendTokenTransaction(receiverKey, amount, gasPrice, gasLimit)
            TransferType.SAFE_ACCOUNT_TOKEN_TRANSFER ->
                sendSafeAccountTokenTransaction(receiverKey, amount, gasPrice, gasLimit)
        }
    }

    fun isTransactionAvailable(isValidated: Boolean) =
        when {
            isMainTransaction || isTokenTransaction -> isValidated && transactionCost < account.cryptoBalance
            else -> isValidated && transactionCost < safeAccountRepository.getSafeAccountMasterOwnerBalance(account.masterOwnerAddress)
        }

    private fun sendSafeAccountTokenTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ) {
        launchDisposable {
            val ownerPrivateKey =
                account.masterOwnerAddress.let { safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(it) }
            transactionRepository.resolveENS(receiverKey)
                .flatMap { resolvedENS ->
                    getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                        .flatMap {
                            transaction = it
                            safeAccountRepository.transferERC20Token(
                                network.chainId,
                                it,
                                tokenAddress
                            ).toSingleDefault(it)
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
                        _sendTransactionLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
                    },
                    onError = {
                        Timber.e("Send safe account transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
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
                account.masterOwnerAddress.let {
                    safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(it)
                }
            transactionRepository.resolveENS(receiverKey)
                .flatMap { resolvedENS ->
                    getTransactionForSafeAccount(ownerPrivateKey, resolvedENS, amount, gasPrice, gasLimit)
                        .flatMap {
                            transaction = it
                            safeAccountRepository.transferNativeCoin(network.chainId, it).toSingleDefault(it)
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
                        _sendTransactionLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
                    },
                    onError = {
                        Timber.e("Send safe account transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
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

    private fun sendMainTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit)
                .flatMap {
                    transaction = it
                    transactionRepository.transferNativeCoin(network.chainId, account.id, it).toSingleDefault(it)
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
                        _saveWalletActionFailedLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
                    }
                )
        }
    }

    private fun sendTokenTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit, tokenAddress)
                .flatMap { transactionRepository.transferERC20Token(account.network.chainId, it).toSingleDefault(it) }
                .onErrorResumeNext { error -> SingleSource { saveTransferFailedWalletAction(error.message) } }
                .flatMapCompletable { saveWalletAction(SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _sendTransactionLiveData.value =
                            Event(Pair("${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}", SENT))
                    },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun getAllAvailableFunds(): String =
        if (recalculateAmount < BigDecimal.ZERO) String.EmptyBalance
        else recalculateAmount.toPlainString()

    val recalculateAmount: BigDecimal
        get() = if (isMainTransaction) cryptoBalance.minus(transactionCost)
        else cryptoBalance

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        transactionRepository.calculateTransactionCost(gasPrice, gasLimit).apply { transactionCost = this }

    fun prepareCurrency() =
        if (tokenAddress != String.Empty) account.getToken(tokenAddress).token.symbol else token

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
        walletActionsRepository.saveWalletActions(
            listOf(
                getAccountsWalletAction(
                    transaction,
                    prepareCurrency(),
                    status
                )
            )
        )

    private fun prepareTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        contractAddress: String = String.Empty
    ): Transaction =
        Transaction(
            account.address,
            account.privateKey,
            receiverKey,
            amount,
            gasPrice,
            gasLimit,
            contractAddress,
            tokenDecimals = tokenDecimals
        )

    fun recalculateFiatAmount(amount: BigDecimal): BigDecimal =
        when (fiatRate) {
            Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
            else -> MarketUtils.calculateFiatBalance(amount, fiatRate)
        }


    fun updateFiatRate() {
        fiatRate = if (tokenAddress != String.Empty) {
            account.getToken(tokenAddress).tokenPrice ?: Double.InvalidValue
        } else {
            coinFiatRate
        }
    }

    companion object {
        const val ONE_ELEMENT = 1
        const val INDEX_OF_NATIVE_COIN = 0
    }
}