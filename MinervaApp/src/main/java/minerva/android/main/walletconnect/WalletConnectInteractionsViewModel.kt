package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prettymuchbryce.abidecoder.Decoder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.*
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.crypto.hexToBigInteger
import minerva.android.kotlinUtils.crypto.toHexString
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.contract.ContractTransactions
import minerva.android.walletmanager.model.contract.TokenStandardJson
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.TokenTransaction
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.utils.TokenUtils.generateTokenHash
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    internal var currentDappSession: DappSession? = null
    private var currentRate: BigDecimal = Double.InvalidValue.toBigDecimal()
    private lateinit var currentTransaction: WalletConnectTransaction
    internal lateinit var currentAccount: Account
    private var tokenDecimal: Int = 0

    private val _walletConnectStatus = MutableLiveData<WalletConnectState>()
    val walletConnectStatus: LiveData<WalletConnectState> get() = _walletConnectStatus

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun dispose() {
        walletConnectRepository.dispose()
    }

    init {
        launchDisposable {
            walletConnectRepository.getSessions()
                .map { reconnect(it) }
                .toFlowable()
                .switchMap { walletConnectRepository.getSessionsFlowable() }
                .filter { it.isNotEmpty() }
                .take(1)
                .switchMap { walletConnectRepository.connectionStatusFlowable }
                .flatMapSingle { mapRequests(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _walletConnectStatus.value = it },
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun reconnect(dapps: List<DappSession>) {
        dapps.forEach { session ->
            with(session) {
                walletConnectRepository.connect(
                    WalletConnectSession(topic, version, bridge, key),
                    peerId,
                    remotePeerId,
                    dapps
                )
            }
        }
    }

    private fun mapRequests(status: WalletConnectStatus) =
        when (status) {
            is OnEthSign ->
                walletConnectRepository.getDappSessionById(status.peerId)
                    .map { session ->
                        currentDappSession = session
                        OnEthSignRequest(status.message, session)
                    }
            is OnDisconnect -> Single.just(OnDisconnected())
            is OnEthSendTransaction -> {
                walletConnectRepository.getDappSessionById(status.peerId)
                    .flatMap { session -> getTransactionCosts(session, status) }
            }
            is OnFailure -> Single.just(if (status.sessionName.isNotEmpty()) OnGeneralError(status.error) else DefaultRequest)
            else -> Single.just(DefaultRequest)
        }

    private fun getValueInFiat(transferType: TransferType, status: OnEthSendTransaction): BigDecimal =
        status.transaction.run {
            if (currentRate == Double.InvalidValue.toBigDecimal()) return currentRate
            if (isTokenTransaction(transferType)) tokenTransaction.tokenValue.toBigDecimal().multiply(currentRate)
            else value?.toBigDecimal()?.multiply(currentRate) ?: Double.InvalidValue.toBigDecimal()
        }

    private fun getConstInFiat(cost: BigDecimal) =
        if (currentRate == Double.InvalidValue.toBigDecimal()) Double.InvalidValue.toBigDecimal()
        else cost.multiply(currentRate)

    private fun getFiatRate(chainId: Int, transferType: TransferType, status: OnEthSendTransaction): Single<Double> =
        if (isTokenTransaction(transferType)) transactionRepository.getTokenFiatRate(getTokenHash(chainId, status))
        else transactionRepository.getCoinFiatRate(chainId)

    private fun getTokenHash(chainId: Int, status: OnEthSendTransaction): String =
        currentAccount.accountTokens
            .find { it.token.symbol == status.transaction.tokenTransaction.tokenSymbol }?.token?.address.let { tokenAddress ->
                generateTokenHash(chainId, tokenAddress ?: String.Empty)
            }

    private fun isTokenTransaction(transferType: TransferType) =
        (transferType == TransferType.TOKEN_SWAP || transferType == TransferType.TOKEN_TRANSFER)

    private fun getTransactionCosts(session: DappSession, status: OnEthSendTransaction): Single<WalletConnectState> {
        currentDappSession = session
        transactionRepository.getAccountByAddress(session.address)?.let { currentAccount = it }
        val txValue: BigDecimal = getTransactionValue(status)
        val transferType: TransferType = getTransferType(status, txValue)
        status.transaction.value = txValue.toPlainString()
        val txCostPayload = getTxCostPayload(session.chainId, status, txValue, transferType)
        return transactionRepository.getTransactionCosts(txCostPayload)
            .flatMap { transactionCost ->
                getFiatRate(session.chainId, transferType, status)
                    .onErrorResumeNext { Single.just(Double.InvalidValue) }
                    .map { rate ->
                        currentRate = rate.toBigDecimal()
                        val valueInFiat = getValueInFiat(transferType, status)
                        val costInFiat = getConstInFiat(transactionCost.cost)
                        currentTransaction = status.transaction.copy(
                            value = BalanceUtils.getCryptoBalance(txValue),
                            fiatValue = valueInFiat.let { fiat ->
                                BalanceUtils.getFiatBalance(fiat, transactionRepository.getFiatSymbol())
                            },
                            txCost = transactionCost.copy(
                                fiatCost = BalanceUtils.getFiatBalance(costInFiat, transactionRepository.getFiatSymbol())
                            ),
                            transactionType = transferType
                        )
                        OnEthSendTransactionRequest(currentTransaction, session, currentAccount)
                    }
            }
    }

    private fun getTransactionValue(status: OnEthSendTransaction): BigDecimal =
        status.transaction.value?.let { value ->
            val amount = hexToBigInteger(value, BigDecimal.ZERO)
            { error -> logToFirebase("Error type: ${error}; Transaction: ${status.transaction}") }
            transactionRepository.toEther(amount)
        }.orElse {
            BigDecimal.ZERO
        }

    private fun logToFirebase(message: String) {
        FirebaseCrashlytics.getInstance().recordException(Throwable(message))
    }

    private fun getTransferType(status: OnEthSendTransaction, value: BigDecimal): TransferType =
        when {
            isContractDataEmpty(status.transaction) -> TransferType.COIN_TRANSFER
            !isContractDataEmpty(status.transaction) && value != BigDecimal.ZERO -> TransferType.COIN_SWAP
            !isContractDataEmpty(status.transaction) && value == BigDecimal.ZERO -> handleTokenTransactions(status)
            else -> TransferType.DEFAULT_COIN_TX
        }

    private fun handleTokenTransactions(status: OnEthSendTransaction): TransferType =
        try {
            val decoder = Decoder()
            decoder.addAbi(TokenStandardJson.contractTransactions)
            val result: Decoder.DecodedMethod? = decoder.decodeMethod(status.transaction.data)
            result?.let { decoded ->
                when (decoded.name) {
                    ContractTransactions.APPROVE.type -> handleApproveAllowance(status, decoded)
                    ContractTransactions.SWAP_EXACT_TOKENS_FOR_TOKENS.type,
                    ContractTransactions.SWAP_EXACT_TOKENS_FOR_ETH.type -> handleTokenSwap(decoded, status)
                    else -> TransferType.DEFAULT_TOKEN_TX
                }
            }.orElse {
                TransferType.DEFAULT_TOKEN_TX
            }

        } catch (e: RuntimeException) {
            Timber.e(e)
            TransferType.DEFAULT_TOKEN_TX
        }

    private fun handleTokenSwap(decoded: Decoder.DecodedMethod, status: OnEthSendTransaction) =
        when {
            decoded.params.size > TOKEN_SWAP_PARAMS -> {
                val tokenTransaction = TokenTransaction()
                val senderTokenContract =
                    "$HEX_PREFIX${((decoded.params[2].value as Array<*>)[0] as ByteArray).toHexString()}"
                findCurrentToken(senderTokenContract, tokenTransaction)
                (decoded.params[0].value as? BigInteger)?.toBigDecimal()?.let { value ->
                    BalanceUtils.convertFromWei(value, tokenDecimal).also {
                        tokenTransaction.tokenValue = BalanceUtils.getCryptoBalance(it)
                    }
                }
                status.transaction.tokenTransaction = tokenTransaction
                TransferType.TOKEN_SWAP
            }
            else -> TransferType.DEFAULT_TOKEN_TX
        }

    private fun handleApproveAllowance(status: OnEthSendTransaction, decoded: Decoder.DecodedMethod): TransferType {
        findCurrentToken(status.transaction.to, status.transaction.tokenTransaction)
        return when {
            decoded.params.size > 1 -> getTransactionTypeBasedOnAllowance(decoded, status)
            else -> TransferType.DEFAULT_TOKEN_TX
        }
    }

    private fun findCurrentToken(tokenAddress: String, tokenTransaction: TokenTransaction) {
        currentAccount.accountTokens
            .find { it.token.address == tokenAddress }
            ?.let {
                tokenTransaction.tokenSymbol = it.token.symbol
                tokenDecimal = it.token.decimals.toInt()
            }
    }

    private fun getTransactionTypeBasedOnAllowance(decoded: Decoder.DecodedMethod, status: OnEthSendTransaction) =
        if (isAllowanceUnlimited(decoded)) {
            status.transaction.tokenTransaction.allowance = Double.InvalidValue.toBigDecimal()
            TransferType.TOKEN_SWAP_APPROVAL
        } else {
            (decoded.params[1].value as? BigInteger)?.toBigDecimal()?.let {
                status.transaction.tokenTransaction.allowance = transactionRepository.toEther(it)
                TransferType.TOKEN_SWAP_APPROVAL
            }.orElse {
                TransferType.DEFAULT_TOKEN_TX
            }
        }

    private fun isAllowanceUnlimited(decoded: Decoder.DecodedMethod) =
        (decoded.params[1].value as? BigInteger)?.toString() == UNLIMITED

    private fun getTxCostPayload(
        chainId: Int,
        status: OnEthSendTransaction,
        value: BigDecimal,
        transferType: TransferType
    ): TxCostPayload =
        TxCostPayload(
            transferType,
            status.transaction.from,
            status.transaction.to,
            value,
            status.transaction.tokenTransaction.allowance,
            chainId,
            tokenDecimal,
            contractData = getContractData(status.transaction)
        )

    private fun getContractData(transaction: WalletConnectTransaction): String =
        if (isContractDataEmpty(transaction)) String.Empty
        else transaction.data

    private fun isContractDataEmpty(transaction: WalletConnectTransaction) =
        hexToBigInteger(transaction.data, BigDecimal.ZERO) == BigDecimal.ZERO

    fun sendTransaction() {
        launchDisposable {
            transactionRepository.sendTransaction(currentAccount.network.chainId, transaction)
                .map { txReceipt ->
                    currentDappSession?.let { session ->
                        walletConnectRepository.approveTransactionRequest(session.peerId, txReceipt)
                    }
                    txReceipt
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _walletConnectStatus.value = ProgressBarState(true) }
                .doOnError {
                    currentDappSession?.let { session ->
                        walletConnectRepository.rejectRequest(session.peerId)
                    }
                    _walletConnectStatus.value = ProgressBarState(false)
                }
                .subscribeBy(
                    onSuccess = { _walletConnectStatus.value = ProgressBarState(false) },
                    onError = { error ->
                        Timber.e(error)
                        _walletConnectStatus.value = OnGeneralError(error)
                    }
                )
        }
    }

    private val transaction
        get() = with(currentTransaction) {
            Transaction(
                from,
                currentAccount.privateKey,
                to,
                BigDecimal(value),
                txCost.gasPrice,
                txCost.gasLimit,
                String.Empty,
                data
            )
        }

    fun recalculateTxCost(gasPrice: BigDecimal, transaction: WalletConnectTransaction): WalletConnectTransaction {
        val txCost = transactionRepository.calculateTransactionCost(gasPrice, transaction.txCost.gasLimit)
        val fiatTxCost = BalanceUtils.getFiatBalance(txCost.multiply(currentRate), transactionRepository.getFiatSymbol())
        currentTransaction = transaction.copy(txCost = transaction.txCost.copy(cost = txCost, fiatCost = fiatTxCost))
        return currentTransaction
    }

    fun acceptRequest() {
        currentDappSession?.let { session ->
            transactionRepository.getAccountByAddress(session.address)?.let {
                walletConnectRepository.approveRequest(session.peerId, it.privateKey)
            }
        }
    }

    fun rejectRequest() {
        currentDappSession?.let {
            walletConnectRepository.rejectRequest(it.peerId)
        }
    }

    fun isBalanceTooLow(balance: BigDecimal, cost: BigDecimal): Boolean =
        balance < cost || balance == BigDecimal.ZERO

    companion object {
        private const val UNLIMITED = "115792089237316195423570985008687907853269984665640564039457584007913129639935"
        private const val TOKEN_SWAP_PARAMS = 2
    }
}