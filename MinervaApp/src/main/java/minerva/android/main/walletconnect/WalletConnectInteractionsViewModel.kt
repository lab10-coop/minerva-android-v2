package minerva.android.main.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.prettymuchbryce.abidecoder.Decoder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.*
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidBigDecimal
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.HEX_PREFIX
import minerva.android.kotlinUtils.crypto.containsHexPrefix
import minerva.android.kotlinUtils.crypto.hexToBigDecimal
import minerva.android.kotlinUtils.crypto.toHexString
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.contract.ContractTransactions
import minerva.android.walletmanager.model.contract.TokenStandardJson
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest as OnSessionRequestData
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequestV2 as OnSessionRequestDataV2
// todo: these includes are super confusing, fix it.
import minerva.android.accounts.walletconnect.OnSessionRequest as OnSessionRequestResult
import minerva.android.accounts.walletconnect.OnSessionRequestV2 as OnSessionRequestResultV2
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.utils.TokenUtils
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectInteractionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletConnectRepository: WalletConnectRepository,
    private val logger: Logger,
    private val tokenManager: TokenManager,
    private val accountManager: AccountManager,
    walletActionsRepository: WalletActionsRepository,
    unsupportedNetworkRepository: UnsupportedNetworkRepository,
    override var address: String
) : BaseWalletConnectScannerViewModel(
    accountManager,
    walletConnectRepository,
    logger,
    walletActionsRepository,
    unsupportedNetworkRepository
) {
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

    /**
     * Change Wallet Connect State - method which update of viewModel state
     * @param state - new (viewModel::_walletConnectStatus) state
     */
    fun changeWalletConnectState(state: WalletConnectState) {
        if (state != _walletConnectStatus.value) {
            _walletConnectStatus.value = state
        }
    }

    fun dispose() {
        walletConnectRepository.dispose()
        onCleared()
    }

    // todo: check if V2 are wrongly getting lost here somewhere.
    fun getWalletConnectSessions() {
        launchDisposable {
            walletConnectRepository.getSessions()
                .map { dappSessions ->
                    reconnect(
                        dappSessions.filterIsInstance<DappSessionV1>()
                    )
                }
                .toFlowable()
                .switchMap { walletConnectRepository.getSessionsFlowable() }
                .filter { dappSessions -> dappSessions.isNotEmpty() }
                .take(1)
                .switchMap { walletConnectRepository.connectionStatusFlowable }
                .flatMapSingle { status -> mapStatus(status) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { state ->
                        if (_walletConnectStatus.value != state) {
                            _walletConnectStatus.value = state
                        }
                    },
                    onError = { error ->
                        Timber.e(error)
                        _errorLiveData.value = Event(error)
                    }
                )
        }
    }

    private fun reconnect(dapps: List<DappSessionV1>) {
        dapps.forEach { session ->
            with(session) {
                walletConnectRepository.connect(
                    WalletConnectSession(topic, version, key, bridge, isMobileWalletConnect = isMobileWalletConnect),
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
            is OnEthSignV2 -> Single.just(OnEthSignRequestV2(status.message, status.session))
            is OnDisconnect -> Single.just(OnDisconnected(sessionName = status.sessionName))
            is OnEthSendTransactionV1 -> {
                walletConnectRepository.getDappSessionById(status.peerId)
                    .flatMap { session -> getTransactionCosts(session, status) }
            }
            is OnEthSendTransactionV2 -> {
                getTransactionCosts(status.session, status)
            }
            is OnFailure -> Single.just(if (status.sessionName.isNotEmpty()) OnGeneralError(status.error) else DefaultRequest)
            else -> Single.just(DefaultRequest)
        }

    private fun getTransactionCosts(session: DappSession, status: OnEthSendTransaction): Single<WalletConnectState> {
        currentDappSession = session
        transactionRepository.getAccountByAddressAndChainId(session.address, session.chainId)
            ?.let { account -> currentAccount = account }
        val txValue: BigDecimal = getTransactionValue(status.transaction.value)
        if (txValue == WRONG_TX_VALUE) {
            rejectRequest(session.isMobileWalletConnect)
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
                        OnEthSendTransactionRequest(currentTransaction, session, currentAccount)
                    }
            }
    }

    private fun getTransactionValue(txValue: String?): BigDecimal =
        txValue?.let { txAmount ->
            val parsedTxValue = hexToBigDecimal(txAmount, WRONG_TX_VALUE)
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
        BalanceUtils.getFiatBalance(getCostInFiat(transactionCost.cost), transactionRepository.getFiatSymbol(), rounding = true)

    private fun getFiatTransactionValue(transferType: TransferType, status: OnEthSendTransaction) =
        BalanceUtils.getFiatBalance(getValueInFiat(transferType, status), transactionRepository.getFiatSymbol(), rounding = true)

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
            .let { tokenAddress -> TokenUtils.generateTokenHash(chainId, tokenAddress ?: String.Empty) }

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
        tokenManager.getActiveTokensPerAccount(currentAccount)
            .find { token -> token.address.equals(tokenAddress, true) }
            ?.let { token ->
                tokenTransaction.tokenSymbol = token.symbol
                tokenDecimal = token.decimals.toInt()
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
        hexToBigDecimal(transaction.data, BigDecimal.ZERO) == BigDecimal.ZERO

    fun sendTransaction(isMobileWalletConnect: Boolean) {
        launchDisposable {
            transactionRepository.sendTransaction(currentAccount.network.chainId, transaction)
                .map { txReceipt ->
                    logToFirebase("Transaction sent by WalletConnect: ${currentTransaction}, receipt: $txReceipt")
                    weiCoinTransactionValue = NO_COIN_TX_VALUE
                    currentDappSession?.let { session ->
                        when {
                            session is DappSessionV1 ->
                                walletConnectRepository.approveTransactionRequest(session.peerId, txReceipt)
                            session is DappSessionV2 ->
                                walletConnectRepository.approveTransactionRequestV2(session.topic, txReceipt)
                        }
                    }
                    txReceipt
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _walletConnectStatus.value = ProgressBarState(true) }
                .doOnError {
                    rejectRequest(isMobileWalletConnect)
                    _walletConnectStatus.value = ProgressBarState(false)
                }
                .doAfterTerminate { weiCoinTransactionValue = NO_COIN_TX_VALUE }
                .subscribeBy(
                    onSuccess = { successWalletConnectInteraction(isMobileWalletConnect) },
                    onError = { error ->
                        logToFirebase("WalletConnect transaction error: $error; $currentTransaction")
                        Timber.e(error)
                        _walletConnectStatus.value = OnWalletConnectTransactionError(error)
                    }
                )
        }
    }

    private fun successWalletConnectInteraction(shouldCloseApp: Boolean) {
        _walletConnectStatus.value = if (shouldCloseApp) CloseScannerState else CloseDialogState
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

    fun acceptRequest(isMobileWalletConnect: Boolean) {
        currentDappSession?.let { session ->
            // todo: why chainid? private key should be the same by address..
            transactionRepository.getAccountByAddressAndChainId(session.address, session.chainId)?.let {
                walletConnectRepository.approveRequest(session.peerId, it.privateKey)
                successWalletConnectInteraction(isMobileWalletConnect)
            }
        }
    }

    fun acceptRequestV2(session: DappSessionV2) {
        Timber.i("asdfsadf ${session.address} ${session.chainId}")
        // todo: why chainid? private key should be the same by address..
        transactionRepository.getAccountByAddressAndChainId(session.address, session.chainId)?.let {
            Timber.i("asdfsadf2")
            walletConnectRepository.approveRequestV2(session.topic, it.privateKey)
            successWalletConnectInteraction(session.isMobileWalletConnect)
        }
    }

    fun rejectRequest(isMobileWalletConnect: Boolean) {
        weiCoinTransactionValue = NO_COIN_TX_VALUE
        currentDappSession?.let { session ->
            when {
                session is DappSessionV1 ->
                    walletConnectRepository.rejectRequest(session.peerId)
                session is DappSessionV2 ->
                    walletConnectRepository.rejectRequestV2(session.topic)
            }
            successWalletConnectInteraction(isMobileWalletConnect)
        }
    }

    fun rejectRequestV2(session: DappSessionV2) {
        weiCoinTransactionValue = NO_COIN_TX_VALUE
        walletConnectRepository.rejectRequestV2(session.topic)
        successWalletConnectInteraction(session.isMobileWalletConnect)
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

    override var account: Account = Account(Int.InvalidId)

    override fun hideProgress() {}

    override fun setLiveDataOnDisconnected(sessionName: String) {
        _walletConnectStatus.value = OnDisconnected(sessionName)
    }

    override fun setLiveDataOnConnectionError(error: Throwable, sessionName: String) {
        _walletConnectStatus.value = OnWalletConnectConnectionError(error, sessionName)
    }

    override fun setLiveDataError(error: Throwable) {
        _errorLiveData.value = Event(error)
    }

    // todo: why is this duplicate with ServicesScannerViewModel??
    override fun handleSessionRequest(sessionRequest: OnSessionRequestData) {
        //if ethereum was chosen set unknown id for showing all networks
        val id: Int? = if (ChainId.ETH_MAIN == sessionRequest.chainId && null == sessionRequest.type) null else sessionRequest.chainId
        when {
            id == null -> {
                accountManager.getFirstActiveAccountOrNull(ChainId.ETH_MAIN)?.let { ethAccount -> account = ethAccount }
                _walletConnectStatus.value = OnSessionRequestResult(
                    sessionRequest.meta,
                    requestedNetwork,
                    WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
                )
            }
            isNetworkNotSupported(chainId = id) -> {
                requestedNetwork = BaseNetworkData(id, String.Empty)
                _walletConnectStatus.value = OnSessionRequestResult(
                    sessionRequest.meta,
                    requestedNetwork,
                    WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
                )
                fetchUnsupportedNetworkName(id)
            }
            else -> _walletConnectStatus.value = getWalletConnectStateForRequestedNetwork(sessionRequest, id)
        }
    }

    // todo: why is this duplicate with ServicesScannerViewModel??
    // todo: implement
    override fun handleSessionRequestV2(sessionRequest: OnSessionRequestDataV2) {
        _walletConnectStatus.value = OnSessionRequestResultV2(
            sessionRequest.meta,
            sessionRequest.numberOfNonEip155Chains,
            sessionRequest.eip155ProposalNamespace
        )
    }

    private fun getWalletConnectStateForRequestedNetwork(
        sessionRequest: OnSessionRequestData,
        chainId: Int
    ): OnSessionRequestResult {
        requestedNetwork = BaseNetworkData(chainId, getNetworkName(chainId))
        return accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
            OnSessionRequestResult(
                sessionRequest.meta,
                requestedNetwork,
                if (null != sessionRequest.type) WalletConnectAlertType.CHANGE_NETWORK else WalletConnectAlertType.CHANGE_ACCOUNT_WARNING
            )
        }.orElse {
            OnSessionRequestResult(
                sessionRequest.meta,
                requestedNetwork,
                WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR
            )
        }
    }

    override fun closeScanner(isMobileWalletConnect: Boolean) {
        successWalletConnectInteraction(isMobileWalletConnect)
    }

    override fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        _walletConnectStatus.postValue(UpdateOnSessionRequest(network, dialogType))
    }

    fun handleDeepLink(deepLink: String) {
        if (!WalletConnectUriUtils.isValidWalletConnectUri(deepLink)) {
            _walletConnectStatus.value = WrongWalletConnectCodeState
        } else {
            _walletConnectStatus.value = CorrectWalletConnectCodeState
            currentSession = walletConnectRepository.getWCSessionFromQr(deepLink)
            walletConnectRepository.connect(currentSession)
        }
    }

    override fun rejectSession(isMobileWalletConnect: Boolean) {
        walletConnectRepository.rejectSession(topic.peerId)
        closeScanner(isMobileWalletConnect)
    }

    override fun rejectSessionV2(proposerPublicKey: String, reason: String, isMobileWalletConnect: Boolean) {
        walletConnectRepository.rejectSessionV2(proposerPublicKey, reason)
        closeScanner(isMobileWalletConnect)
    }
}