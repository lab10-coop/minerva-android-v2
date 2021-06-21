package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.prettymuchbryce.abidecoder.Decoder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.*
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidBigDecimal
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.crypto.containsHexPrefix
import minerva.android.kotlinUtils.crypto.hexToBigInteger
import minerva.android.kotlinUtils.crypto.toHexString
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.utils.logger.Logger
import minerva.android.walletmanager.model.contract.ContractTransactions
import minerva.android.walletmanager.model.contract.TokenStandardJson
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
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
    private val walletConnectRepository: WalletConnectRepository,
    private val logger: Logger
) : BaseViewModel() {

    internal var currentDappSession: DappSession? = null
    private var currentRate: BigDecimal = Double.InvalidBigDecimal
    private lateinit var currentTransaction: WalletConnectTransaction
    internal lateinit var currentAccount: Account
    private var tokenDecimal: Int = DEFAULT_TOKEN_DECIMALS
    private var weiCoinTransactionValue: BigDecimal = NO_COIN_TX_VALUE

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
                .map { dappSessions -> reconnect(dappSessions) }
                .toFlowable()
                .switchMap { walletConnectRepository.getSessionsFlowable() }
                .filter { dappSessions -> dappSessions.isNotEmpty() }
                .take(1)
                .switchMap { walletConnectRepository.connectionStatusFlowable }
                .flatMapSingle { status -> mapStatus(status) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { state -> _walletConnectStatus.value = state },
                    onError = { error ->
                        Timber.e(error)
                        _errorLiveData.value = Event(error)
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

    private fun mapStatus(status: WalletConnectStatus) =
        when (status) {
            is OnEthSign ->
                walletConnectRepository.getDappSessionById(status.peerId)
                    .map { session ->
                        currentDappSession = session
                        OnEthSignRequest(status.message, session)
                    }
            is OnDisconnect -> Single.just(OnDisconnected())
            is OnEthSendTransaction -> {
                logToFirebase("Transaction payload from WalletConnect: ${status.transaction}")
                walletConnectRepository.getDappSessionById(status.peerId)
                    .flatMap { session -> getTransactionCosts(session, status) }
            }
            is OnFailure -> Single.just(if (status.sessionName.isNotEmpty()) OnGeneralError(status.error) else DefaultRequest)
            else -> Single.just(DefaultRequest)
        }

    private fun getTransactionCosts(session: DappSession, status: OnEthSendTransaction): Single<WalletConnectState> {
        currentDappSession = session
        transactionRepository.getAccountByAddressAndChainId(session.address, session.chainId)?.let { account -> currentAccount = account }
        val txValue: BigDecimal = getTransactionValue(status.transaction.value)
        if (txValue == WRONG_TX_VALUE) {
            rejectRequest()
            return Single.just(WrongTransactionValueState(status.transaction))
        }
        val transferType: TransferType = getTransferType(status, txValue)
        status.transaction.value = txValue.toPlainString()
        val txCostPayload = getTxCostPayload(session.chainId, status, txValue, transferType)
        return transactionRepository.getTransactionCosts(txCostPayload)
            .flatMap { transactionCost ->
                getFiatRate(session.chainId, transferType, status)
                    .onErrorResumeNext { Single.just(Double.InvalidValue) }
                    .map { rate ->
                        currentRate = rate.toBigDecimal()
                        currentTransaction = status.transaction.copy(
                            value = BalanceUtils.getCryptoBalance(txValue),
                            fiatValue = getFiatTransactionValue(transferType, status),
                            txCost = transactionCost.copy(fiatCost = getFiatTransactionCost(transactionCost)),
                            transactionType = transferType
                        )
                        logToFirebase("Shown transaction: $currentTransaction")
                        OnEthSendTransactionRequest(currentTransaction, session, currentAccount)
                    }
            }
    }

    private fun getTransactionValue(txValue: String?): BigDecimal =
        txValue?.let { txAmount ->
            val parsedTxValue = hexToBigInteger(txAmount, WRONG_TX_VALUE)
            if (containsHexPrefix(txAmount) && parsedTxValue != WRONG_TX_VALUE) {
                weiCoinTransactionValue = parsedTxValue
                transactionRepository.toUserReadableFormat(parsedTxValue)
            } else {
                WRONG_TX_VALUE
            }
        }.orElse {
            NO_COIN_TX_VALUE
        }

    private fun getFiatTransactionCost(transactionCost: TransactionCost) =
        BalanceUtils.getFiatBalance(getCostInFiat(transactionCost.cost), transactionRepository.getFiatSymbol())

    private fun getFiatTransactionValue(transferType: TransferType, status: OnEthSendTransaction) =
        BalanceUtils.getFiatBalance(getValueInFiat(transferType, status), transactionRepository.getFiatSymbol())

    private fun getValueInFiat(transferType: TransferType, status: OnEthSendTransaction): BigDecimal =
        status.transaction.run {
            return when {
                currentRate == Double.InvalidBigDecimal -> currentRate
                isTokenTransaction(transferType) -> tokenTransaction.tokenValue.toBigDecimal().multiply(currentRate)
                else -> value?.toBigDecimal()?.multiply(currentRate) ?: Double.InvalidBigDecimal
            }
        }

    private fun getCostInFiat(cost: BigDecimal): BigDecimal =
        if (currentRate != Double.InvalidBigDecimal) cost.multiply(currentRate)
        else Double.InvalidBigDecimal

    private fun getFiatRate(chainId: Int, transferType: TransferType, status: OnEthSendTransaction): Single<Double> =
        if (isTokenTransaction(transferType)) transactionRepository.getTokenFiatRate(getTokenHash(chainId, status))
        else transactionRepository.getCoinFiatRate(chainId)

    private fun getTokenHash(chainId: Int, status: OnEthSendTransaction): String =
        currentAccount.accountTokens
            .find { it.token.symbol == status.transaction.tokenTransaction.tokenSymbol }?.token?.address
            .let { tokenAddress -> generateTokenHash(chainId, tokenAddress ?: String.Empty) }

    private fun isTokenTransaction(transferType: TransferType) =
        (transferType == TransferType.TOKEN_SWAP || transferType == TransferType.TOKEN_TRANSFER)

    private fun getTransferType(status: OnEthSendTransaction, value: BigDecimal): TransferType =
        when {
            isContractDataEmpty(status.transaction) -> TransferType.COIN_TRANSFER
            !isContractDataEmpty(status.transaction) && value != NO_COIN_TX_VALUE -> TransferType.COIN_SWAP
            !isContractDataEmpty(status.transaction) && value == NO_COIN_TX_VALUE -> {
                weiCoinTransactionValue = NO_COIN_TX_VALUE
                handleTokenTransactions(status)
            }
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

    private fun handleTokenSwap(decoded: Decoder.DecodedMethod, status: OnEthSendTransaction): TransferType =
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
            .find { accountToken -> accountToken.token.address.equals(tokenAddress, true) }
            ?.let { accountToken ->
                tokenTransaction.tokenSymbol = accountToken.token.symbol
                tokenDecimal = accountToken.token.decimals.toInt()
            }.orElse {
                tokenDecimal = DEFAULT_TOKEN_DECIMALS
            }
    }

    private fun getTransactionTypeBasedOnAllowance(decoded: Decoder.DecodedMethod, status: OnEthSendTransaction) =
        if (isAllowanceUnlimited(decoded)) {
            status.transaction.tokenTransaction.allowance = Double.InvalidBigDecimal
            TransferType.TOKEN_SWAP_APPROVAL
        } else {
            (decoded.params[1].value as? BigInteger)?.toBigDecimal()?.let {
                status.transaction.tokenTransaction.allowance = transactionRepository.toUserReadableFormat(it)
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
                    logToFirebase(
                        "Transaction sent by WalletConnect: ${currentTransaction}, receipt: $txReceipt," +
                                "token transaction: ${currentTransaction.tokenTransaction}"
                    )
                    weiCoinTransactionValue = NO_COIN_TX_VALUE
                    currentDappSession?.let { session ->
                        walletConnectRepository.approveTransactionRequest(session.peerId, txReceipt)
                    }
                    txReceipt
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _walletConnectStatus.value = ProgressBarState(true) }
                .doOnError {
                    rejectRequest()
                    _walletConnectStatus.value = ProgressBarState(false)
                }
                .doAfterTerminate { weiCoinTransactionValue = NO_COIN_TX_VALUE }
                .subscribeBy(
                    onSuccess = { _walletConnectStatus.value = ProgressBarState(false) },
                    onError = { error ->
                        Timber.e(error)
                        _walletConnectStatus.value = OnWalletConnectTransactionError(error)
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
                weiCoinTransactionValue,
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
            transactionRepository.getAccountByAddressAndChainId(session.address, session.chainId)?.let {
                walletConnectRepository.approveRequest(session.peerId, it.privateKey)
            }
        }
    }

    fun rejectRequest() {
        weiCoinTransactionValue = NO_COIN_TX_VALUE
        currentDappSession?.let { session ->
            walletConnectRepository.rejectRequest(session.peerId)
        }
    }

    fun isBalanceTooLow(balance: BigDecimal, cost: BigDecimal): Boolean =
        balance < cost || balance == BigDecimal.ZERO

    fun logToFirebase(message: String) {
        logger.logToFirebase(message)
    }

    companion object {
        private const val UNLIMITED = "115792089237316195423570985008687907853269984665640564039457584007913129639935"
        private const val TOKEN_SWAP_PARAMS = 2
        private const val DEFAULT_TOKEN_DECIMALS = 0
        private val WRONG_TX_VALUE = BigDecimal(-1)
        private val NO_COIN_TX_VALUE = BigDecimal.ZERO
    }
}