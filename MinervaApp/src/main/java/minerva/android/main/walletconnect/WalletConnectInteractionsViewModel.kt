package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.*
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.crypto.hexToBigInteger
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import minerva.android.walletmanager.utils.BalanceUtils
import timber.log.Timber
import java.math.BigDecimal

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    internal lateinit var currentDappSession: DappSession
    private var currentRate: BigDecimal = BigDecimal.ZERO
    private lateinit var currentTransaction: WalletConnectTransaction
    internal lateinit var currentAccount: Account

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

    private fun getTransactionCosts(
        session: DappSession,
        status: OnEthSendTransaction
    ): Single<OnEthSendTransactionRequest> {
        currentDappSession = session
        transactionRepository.getAccountByAddress(currentDappSession.address)?.let { currentAccount = it }
        val value = transactionRepository.toEther(hexToBigInteger(status.transaction.value, BigDecimal.ZERO))
        status.transaction.value = value.toPlainString()
        return transactionRepository.getTransactionCosts(getTxCostPayload(currentDappSession.chainId, status, value))
            .flatMap { transactionCost ->
                transactionRepository.getEurRate(session.chainId)
                    .map {
                        currentRate = it.toBigDecimal()
                        val valueInFiat = status.transaction.value.toBigDecimal().multiply(currentRate)
                        val costInFiat = transactionCost.cost.multiply(currentRate)
                        currentTransaction = status.transaction.copy(
                            fiatValue = BalanceUtils.getFiatBalance(valueInFiat),
                            txCost = transactionCost.copy(fiatCost = BalanceUtils.getFiatBalance(costInFiat))
                        )
                        OnEthSendTransactionRequest(currentTransaction, session, currentAccount)
                    }
            }
    }

    private fun getTxCostPayload(chainId: Int, status: OnEthSendTransaction, value: BigDecimal): TxCostPayload =
        TxCostPayload(
            getTransferType(status, value),
            status.transaction.from,
            status.transaction.to,
            value,
            chainId, //todo handle token decimals ??
            contractAddress = getContractData(status.transaction)
        )

    //TODO include other transaction types (token transfer, token swap)
    private fun getTransferType(status: OnEthSendTransaction, value: BigDecimal): TransferType =
        when {
            isContractDataEmpty(status.transaction) -> TransferType.COIN_TRANSFER
            !isContractDataEmpty(status.transaction) && value != BigDecimal.ZERO -> TransferType.COIN_SWAP
            else -> TransferType.UNDEFINED
        }

    private fun getContractData(transaction: WalletConnectTransaction): String =
        if (isContractDataEmpty(transaction)) String.Empty
        else transaction.data

    private fun isContractDataEmpty(transaction: WalletConnectTransaction) =
        hexToBigInteger(transaction.data, BigDecimal.ZERO) == BigDecimal.ZERO

    fun sendTransaction() {
        launchDisposable {
            transactionRepository.sendTransaction(currentAccount.network.chainId, transaction)
                .map {
                    walletConnectRepository.approveTransactionRequest(currentDappSession.peerId, it)
                    it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _walletConnectStatus.value = ProgressBarState(true) }
                .doOnError {
                    walletConnectRepository.rejectRequest(currentDappSession.peerId)
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

    fun recalculateTxCost(gasPrice: BigDecimal, transaction: WalletConnectTransaction): WalletConnectTransaction {
        val txCost = transactionRepository.calculateTransactionCost(gasPrice, transaction.txCost.gasLimit)
        val fiatTxCost = BalanceUtils.getFiatBalance(txCost.multiply(currentRate))
        currentTransaction = transaction.copy(txCost = transaction.txCost.copy(cost = txCost, fiatCost = fiatTxCost))
        return currentTransaction
    }

    fun acceptRequest() {
        transactionRepository.getAccountByAddress(currentDappSession.address)?.let {
            walletConnectRepository.approveRequest(currentDappSession.peerId, it.privateKey)
        }
    }

    fun rejectRequest() {
        walletConnectRepository.rejectRequest(currentDappSession.peerId)
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

    fun isBalanceTooLow(balance: BigDecimal, cost: BigDecimal): Boolean =
        balance < cost || balance == BigDecimal.ZERO
}