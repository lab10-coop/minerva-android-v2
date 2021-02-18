package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.*
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    lateinit var currentDappSession: DappSession
    private var currentRate: Double = Double.InvalidValue

    private val _walletConnectStatus = MutableLiveData<WalletConnectState>()
    val walletConnectStatus: LiveData<WalletConnectState> get() = _walletConnectStatus

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
                    onError = { Timber.e(it) }
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
                    .flatMap { session -> getTransactionCosts(session, status) }
            else -> Single.just(DefaultRequest)
        }

    private fun getTransactionCosts(
        session: DappSession,
        status: OnEthSendTransaction
    ): Single<OnEthSendTransactionRequest> {
        currentDappSession = session
        val account = transactionRepository.getAccountByAddress(currentDappSession.address)
        if (status.transaction.value == "0x0") status.transaction.value = "0" //todo add parsing
        return transactionRepository.getTransactionCosts(
            account?.network?.short!!,
            Int.InvalidIndex,
            status.transaction.from,
            status.transaction.to,
            status.transaction.value.toBigDecimal(),
            session.chainId
        ).flatMap { transactionCost ->
            transactionRepository.getEurRate(session.chainId)
                .map {
                    currentRate = it
                    val valueInFiat = status.transaction.value.toDouble() * currentRate
                    val costInFiat = transactionCost.cost.multiply(BigDecimal(currentRate))
                    OnEthSendTransactionRequest(
                        status.transaction.copy(
                            fiatValue = valueInFiat,
                            txCost = transactionCost.copy(fiatCost = BalanceUtils.getFiatBalance(costInFiat))
                        ), session, account
                    )
                }
        }
    }

    fun acceptRequest() {
        transactionRepository.getAccountByAddress(currentDappSession.address)?.let {
            walletConnectRepository.approveRequest(currentDappSession.peerId, it.privateKey)
        }
    }

    fun rejectRequest() {
        walletConnectRepository.rejectRequest(currentDappSession.peerId)
    }

    fun recalculateTxCost(gasPrice: BigDecimal, transaction: WalletConnectTransaction): WalletConnectTransaction {
        val txCost = transactionRepository.calculateTransactionCost(gasPrice, transaction.txCost.gasLimit)
        val fiatTxCost = BalanceUtils.getFiatBalance(txCost.multiply(BigDecimal(currentRate)))
        return transaction.copy(txCost = transaction.txCost.copy(cost = txCost, fiatCost = fiatTxCost))
    }

}