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
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.crypto.hexToBigInteger
import minerva.android.kotlinUtils.crypto.toHexString
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.contract.ERC20TRANSACTIONS
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
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    internal var currentDappSession: DappSession? = null
    private var currentRate: BigDecimal = BigDecimal.ZERO
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { reconnect(it) },
                    onError = { Timber.e(it) }
                )
        }
    }

    private fun reconnect(dapps: List<DappSession>) {
        dapps.forEach { session ->
            with(session) {
                walletConnectRepository.connect(WalletConnectSession(topic, version, bridge, key), peerId, remotePeerId)
            }
        }
        subscribeToWalletConnectEvents()
    }

    private fun subscribeToWalletConnectEvents() {
        launchDisposable {
            walletConnectRepository.connectionStatusFlowable
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

    private fun mapRequests(status: WalletConnectStatus) =
        when (status) {
            is OnEthSign ->
                walletConnectRepository.getDappSessionById(status.peerId)
                    .map { session ->
                        currentDappSession = session
                        OnEthSignRequest(status.message, session)
                    }
            is OnDisconnect -> Single.just(OnDisconnected)
            is OnEthSendTransaction ->
                walletConnectRepository.getDappSessionById(status.peerId)
                    .flatMap { session ->
                        getTransactionCosts(session, status)
                    }
            else -> Single.just(DefaultRequest)
        }

    private fun getTransactionCosts(session: DappSession, status: OnEthSendTransaction): Single<WalletConnectState> {
        currentDappSession = session
        transactionRepository.getAccountByAddress(session.address)?.let { currentAccount = it }
        val value = getTransactionValue(status)
        val transferType = getTransferType(status, value)
        status.transaction.value = value.toPlainString()
        val txCostPayload = getTxCostPayload(session.chainId, status, value, transferType)
        return transactionRepository.getTransactionCosts(txCostPayload)
            .flatMap { transactionCost ->
                transactionRepository.getEurRate(session.chainId)
                    .onErrorResumeNext { Single.just(0.0) }
                    .map {
                        currentRate = it.toBigDecimal()
                        val valueInFiat = status.transaction.value?.toBigDecimal()?.multiply(currentRate)!!
                        val costInFiat = transactionCost.cost.multiply(currentRate)
                        when (transferType) {
                            TransferType.UNDEFINED -> OnUndefinedTransaction
                            else -> {
                                currentTransaction = status.transaction.copy(
                                    fiatValue = BalanceUtils.getFiatBalance(valueInFiat),
                                    txCost = transactionCost.copy(fiatCost = BalanceUtils.getFiatBalance(costInFiat)),
                                    transactionType = transferType
                                )
                                OnEthSendTransactionRequest(currentTransaction, session, currentAccount)
                            }
                        }
                    }
            }
    }

    private fun getTransactionValue(status: OnEthSendTransaction) =
        status.transaction.value?.let {
            transactionRepository.toEther(hexToBigInteger(it, BigDecimal.ZERO))
        }.orElse { BigDecimal.ZERO }

    private fun getTransferType(status: OnEthSendTransaction, value: BigDecimal): TransferType =
        when {
            isContractDataEmpty(status.transaction) -> TransferType.COIN_TRANSFER
            !isContractDataEmpty(status.transaction) && value != BigDecimal.ZERO -> TransferType.COIN_SWAP
            !isContractDataEmpty(status.transaction) && value == BigDecimal.ZERO -> handleTokenTransactions(status)
            else -> TransferType.UNDEFINED
        }

    private fun handleTokenTransactions(status: OnEthSendTransaction): TransferType =
        try {
            val decoder = Decoder()
            decoder.addAbi(TokenStandardJson.erc20TokenTransactionsAbi)
            val result: Decoder.DecodedMethod? = decoder.decodeMethod(status.transaction.data)
            result?.let { decoded ->
                when (decoded.name) {
                    ERC20TRANSACTIONS.APPROVE.type -> handleApproveAllowance(status, decoded)
                    ERC20TRANSACTIONS.SWAP_EXACT_TOKENS_FOR_TOKENS.type,
                    ERC20TRANSACTIONS.SWAP_EXACT_TOKENS_FOR_ETH.type -> handleTokenSwap(decoded, status)
                    else -> TransferType.UNDEFINED
                }
            }.orElse {
                TransferType.UNDEFINED
            }

        } catch (e: RuntimeException) {
            Timber.e(e)
            TransferType.UNDEFINED
        }

    private fun handleTokenSwap(decoded: Decoder.DecodedMethod, status: OnEthSendTransaction) =
        when {
            decoded.params.size > TOKEN_SWAP_PARAMS -> {
                val tokenTransaction = TokenTransaction()
                val senderTokenContract =
                    "$HEX_PREFIX${((decoded.params[2].value as Array<*>)[0] as ByteArray).toHexString()}"
                findCurrentToken(senderTokenContract, tokenTransaction)

                (decoded.params[0].value as? BigInteger)?.toBigDecimal()?.let {
                    tokenTransaction.tokenValue = BalanceUtils.fromWei(it, tokenDecimal).toPlainString()
                }

                status.transaction.tokenTransaction = tokenTransaction
                TransferType.TOKEN_SWAP
            }
            else -> TransferType.UNDEFINED
        }

    private fun handleApproveAllowance(status: OnEthSendTransaction, decoded: Decoder.DecodedMethod): TransferType {
        findCurrentToken(status.transaction.to, status.transaction.tokenTransaction)
        return when {
            decoded.params.size > 1 -> getTransactionTypeBasedOnAllowance(decoded, status)
            else -> TransferType.UNDEFINED
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
            status.transaction.tokenTransaction.allowance = Int.InvalidValue.toBigDecimal()
            TransferType.TOKEN_SWAP_APPROVAL
        } else {
            (decoded.params[1].value as? BigInteger)?.toBigDecimal()?.let {
                status.transaction.tokenTransaction.allowance = transactionRepository.toEther(it)
                TransferType.TOKEN_SWAP_APPROVAL
            }.orElse {
                TransferType.UNDEFINED
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
                .map {
                    currentDappSession?.let { session ->
                        walletConnectRepository.approveTransactionRequest(session.peerId, it)
                    }
                    it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _walletConnectStatus.value = ProgressBarState(true) }
                .doOnError {
                    currentDappSession?.let {
                        walletConnectRepository.rejectRequest(it.peerId)
                    }
                    _walletConnectStatus.value = ProgressBarState(false)
                }
                .subscribeBy(
                    onSuccess = { _walletConnectStatus.value = ProgressBarState(false) },
                    onError = {
                        Timber.e(it)
                        _walletConnectStatus.value = OnError(it)
                    }
                )
        }
    }

    fun killSession() {
        currentDappSession?.let {
            launchDisposable {
                walletConnectRepository.killSession(it.peerId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onError = { Timber.e(it) })
            }
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
        val fiatTxCost = BalanceUtils.getFiatBalance(txCost.multiply(currentRate))
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
        private const val UNLIMITED = "-1"
        private const val TOKEN_SWAP_PARAMS = 2
    }
}