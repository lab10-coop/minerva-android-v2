package minerva.android.accounts.nft.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.nft.model.NftItem
import minerva.android.base.BaseViewModel
import minerva.android.extension.fromJsonArrayToList
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.transactions.TxSpeed
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.utils.BalanceUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber
import java.lang.RuntimeException
import java.math.BigDecimal
import java.math.BigInteger

class NftCollectionViewModel(
    private val accountManager: AccountManager,
    private val tokenManager: TokenManager,
    private val transactionRepository: TransactionRepository,
    private val walletActionsRepository: WalletActionsRepository,
    private val accountId: Int,
    private val collectionAddress: String, //json with address(es) of token
    private val isGroup: Boolean //combine all favorites token like one item
) : BaseViewModel() {

    private val nftList = mutableListOf<NftItem>()
    private var initialGasLimit = BigInteger.ONE

    var recentSelectedTxSpeed: TxSpeed? = null

    @VisibleForTesting
    lateinit var transaction: Transaction

    val account: Account get() = accountManager.loadAccount(accountId)
    val token: String get() = account.network.token

    var selectedItem: NftItem = NftItem.Invalid
        set(value) {
            field = value
            _selectedItemLiveData.value = selectedItem
        }

    var transactionCost: TransactionCost = TransactionCost()
        set(value) {
            field = value
            _transactionCostLiveData.value = Event(value)
        }

    private val _nftListLiveData = MutableLiveData<List<NftItem>>(nftList)
    val nftListLiveData: LiveData<List<NftItem>> get() = _nftListLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean> get() = _loadingLiveData

    private val _selectedItemLiveData = MutableLiveData<NftItem>(selectedItem)
    val selectedItemLiveData: LiveData<NftItem> get() = _selectedItemLiveData

    private val _saveWalletActionFailedLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val saveWalletActionFailedLiveData: LiveData<Event<Pair<String, Int>>> get() = _saveWalletActionFailedLiveData

    private val _sendTransactionLoadingLiveData = MutableLiveData<Event<Boolean>>()
    val sendTransactionLoadingLiveData: LiveData<Event<Boolean>> get() = _sendTransactionLoadingLiveData

    private val _transactionCostLoadingLiveData = MutableLiveData<Event<Boolean>>()
    val transactionCostLoadingLiveData: LiveData<Event<Boolean>> get() = _transactionCostLoadingLiveData

    private val _sendTransactionLiveData = MutableLiveData<Event<Pair<String, Int>>>()
    val sendTransactionLiveData: LiveData<Event<Pair<String, Int>>> get() = _sendTransactionLiveData

    private val _transactionCompletedLiveData = MutableLiveData<Event<Any>>()
    val transactionCompletedLiveData: LiveData<Event<Any>> get() = _transactionCompletedLiveData

    private val _transactionCostLiveData = MutableLiveData<Event<TransactionCost>>()
    val transactionCostLiveData: LiveData<Event<TransactionCost>> get() = _transactionCostLiveData

    private val _transactionSpeedListLiveData = MutableLiveData<Event<List<TxSpeed>>>()
    val transactionSpeedListLiveData: LiveData<Event<List<TxSpeed>>> get() = _transactionSpeedListLiveData

    private val _transactionCostLoadingErrorLiveData = MutableLiveData<Event<Throwable>>()
    val transactionCostLoadingErrorLiveData: LiveData<Event<Throwable>> get() = _transactionCostLoadingErrorLiveData

    fun setGasLimit(gasLimit: BigInteger) {
        transactionCost = transactionCost.copy(gasLimit = gasLimit)
    }

    fun restoreGasLimit() {
        transactionCost = transactionCost.copy(gasLimit = initialGasLimit)
    }

    fun setGasPrice(gasPrice: BigDecimal) {
        transactionRepository.calculateTransactionCost(
            gasPrice,
            transactionCost.gasLimit
        ).let { txCost ->
            transactionCost = transactionCost.copy(
                cost = txCost,
                fiatCost = BalanceUtils.getFiatBalance(
                    txCost.multiply(account.coinRate.toBigDecimal()),
                    transactionRepository.getFiatSymbol()
                ),
                gasPrice = gasPrice
            )
        }

    }

    fun sendTransaction(receiverKey: String, amount: BigDecimal) {
        sendTransferTransaction(
            receiverKey,
            amount,
            transactionCost.gasPrice,
            transactionCost.gasLimit,
            transferType
        )
    }


    private fun resolveENS(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ): Single<Transaction> =
        transactionRepository
            .resolveENS(receiverKey)
            .map {
                prepareTransaction(it, amount, gasPrice, gasLimit)
                    .also { tx -> transaction = tx }
            }


    private fun sendTransferTransaction(
        receiverKey: String,
            amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        transferType: TransferType
    ) {
        launchDisposable {
            resolveENS(receiverKey, amount, gasPrice, gasLimit)
                .flatMap {
                    transaction = it
                    when (transferType) {
                        TransferType.ERC721_TRANSFER -> transactionRepository.transferERC721Token(
                            account.network.chainId,
                            it
                        ).toSingleDefault(it)
                        TransferType.ERC1155_TRANSFER -> transactionRepository.transferERC1155Token(
                            account.network.chainId,
                            it
                        ).toSingleDefault(it)
                        else -> throw RuntimeException("Invalid TransferType")
                    }

                }
                .onErrorResumeNext { error -> SingleSource { saveTransferFailedWalletAction(error.message) } }
                .flatMapCompletable { saveWalletAction(WalletActionStatus.SENT, it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _sendTransactionLoadingLiveData.value = Event(true) }
                .doOnEvent { _sendTransactionLoadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _transactionCompletedLiveData.value = Event(Any())
                        selectedItem.wasSent = true
                        selectedItem.balance = selectedItem.balance - amount
                        updateList()
                    },
                    onError = {
                        Timber.e("Send transaction error: ${it.message}")
                        _saveWalletActionFailedLiveData.value = Event(
                            Pair(
                                "${BalanceUtils.getCryptoBalance(amount)} ${prepareCurrency()}",
                                WalletActionStatus.SENT
                            )
                        )
                    }
                )
        }
    }

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


    private fun prepareCurrency() = if (selectedItem.tokenAddress != String.Empty)
        account.getToken(selectedItem.tokenAddress).token.symbol
    else
        token


    private fun getAccountsWalletAction(
        transaction: Transaction,
        token: String,
        status: Int
    ): WalletAction = WalletAction(
        WalletActionType.ACCOUNT,
        status,
        DateUtils.timestamp,
        hashMapOf(
            Pair(WalletActionFields.AMOUNT, transaction.amount.toPlainString()),
            Pair(WalletActionFields.TOKEN, token),
            Pair(WalletActionFields.RECEIVER, transaction.receiverKey)
        )
    )

    private fun saveTransferFailedWalletAction(message: String?) {
        launchDisposable {
            saveWalletAction(WalletActionStatus.FAILED, transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _sendTransactionLoadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        Timber.e(message)
                        _sendTransactionLiveData.value = Event(
                            Pair(
                                message ?: "${transaction.amount} $token",
                                WalletActionStatus.FAILED
                            )
                        )
                    },
                    onError = {
                        Timber.e("Save wallet action error $it")
                        _transactionCostLoadingErrorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun getNftForCollection() {
        _loadingLiveData.value = true
        accountManager.loadAccount(accountId).let { account ->
            val visibleTokens: MutableList<AccountToken> = account.getVisibleTokens().toMutableList()
            //clear previous data for prevent recurring items
            nftList.clear()
            //get list of favorite tokens addresses from json wrapper(json array)
            val favoriteTokenAddresses: List<String> = collectionAddress.fromJsonArrayToList()
            //get accountsToken from account according to addresses which came from favoriteTokenAddresses
            account.accountTokens.forEach { accountToken ->
                favoriteTokenAddresses.forEach { favTokenAddress ->
                    if (accountToken.token.address.equals(favTokenAddress))
                        visibleTokens.add(accountToken)
                }
            }

            favoriteTokenAddresses.forEach { favoriteTokenAddress ->
                tokenManager.getNftsPerAccount(account.chainId, account.address, favoriteTokenAddress).forEach { token ->
                    with(token) {
                        val filteredVisibleTokens: AccountToken?

                        if (isGroup) //show only favorite token/nft
                            filteredVisibleTokens = visibleTokens
                                .find { accountToken -> tokenId == accountToken.token.tokenId && isFavorite }
                        else
                            filteredVisibleTokens = visibleTokens
                                .find { accountToken -> tokenId == accountToken.token.tokenId }
                        //filling main token/nft list
                        filteredVisibleTokens?.let {
                            nftList.add(
                                NftItem(
                                    address,
                                    tokenId ?: String.Empty,
                                    nftContent,
                                    name,
                                    type.isERC1155(),
                                    decimals,
                                    it.currentBalance,
                                    isFavorite = it.token.isFavorite))
                        }
                    }
                }
            }
            updateList()
        }
    }

    fun selectItem(item: NftItem) {
        selectedItem = item
    }

    private fun Account.getVisibleTokens() = accountTokens.filter { accountToken ->
        accountToken.token.address.equals(
            collectionAddress,
            true
        ) && accountToken.token.type.isNft() && accountToken.currentRawBalance > BigDecimal.ZERO
    }

    private fun updateList() {
        _loadingLiveData.value = false
        _nftListLiveData.value = nftList.filter { !it.wasSent || it.balance > BigDecimal.ZERO}.sortedBy { it.tokenId }
    }

    fun getTransactionCosts(to: String, amount: BigDecimal) {
        launchDisposable {
            transactionRepository
                .getTransactionCosts(getTxCostPayload(to, amount))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    _transactionCostLoadingLiveData.value = Event(true)
                }
                .doOnEvent { _, _ -> _transactionCostLoadingLiveData.value = Event(false) }
                .onErrorReturn {
                    transactionCost = TransactionCost()
                    initialGasLimit = BigInteger.ONE
                    _transactionCostLoadingErrorLiveData.value = Event(it)
                    transactionCost
                }
                .subscribeBy (
                    onSuccess = {
                        transactionCost = it
                        initialGasLimit = it.gasLimit
                        _transactionSpeedListLiveData.value = Event(it.txSpeeds)
                    }
                )
        }
    }


    private fun prepareTransaction(
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ): Transaction = Transaction(
        account.address,
        account.privateKey,
        receiverKey,
        amount,
        gasPrice,
        gasLimit,
        contractAddress = selectedItem.tokenAddress,
        tokenDecimals = selectedItem.decimals.toIntOrNull() ?: DEFAULT_DECIMALS,
        tokenId = selectedItem.tokenId
    )


    private fun getTxCostPayload(to: String, amount: BigDecimal): TxCostPayload = TxCostPayload(
        transferType,
        account.address,
        to,
        amount,
        chainId = account.network.chainId,
        tokenDecimals = selectedItem.decimals.toIntOrNull() ?: DEFAULT_DECIMALS,
        contractAddress = selectedItem.tokenAddress
    )

    private val transferType: TransferType
        get() = when (selectedItem.isERC1155) {
            true -> TransferType.ERC1155_TRANSFER
            false -> TransferType.ERC721_TRANSFER
        }

    fun isAddressValid(address: String): Boolean =
        transactionRepository.isAddressValid(address, account.chainId)

    /**
     * Change Favorite State - change favorite state of selected nft
     * @param nftItem - item which value will be changed
     */
    fun changeFavoriteState(nftItem: NftItem) = launchDisposable {
            accountManager.changeFavoriteState(account, nftItem.tokenId, !nftItem.isFavorite)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    getNftForCollection()
                }
        }

    fun isAmountAvailable() = selectedItem.isERC1155 && selectedItem.balance > BigDecimal.ONE
    fun isTransactionAvailable(isValidated: Boolean) = isValidated && isAccountBalanceEnough()
    fun isAuthenticationEnabled(): Boolean = transactionRepository.isProtectTransactionEnabled()
    fun isAccountBalanceEnough() = transactionCost.cost < account.cryptoBalance
    fun getAllAvailableFunds() = selectedItem.balance

    companion object {
        private const val DEFAULT_DECIMALS = 0
    }
}