package minerva.android.token.ramp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.R
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.token.ramp.model.RampCrypto
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class RampViewModel(
    private val walletActionsRepository: WalletActionsRepository,
    private val accountManager: AccountManager
) : BaseViewModel() {

    var spinnerPosition: Int = DEFAULT_CRYPTO_POSITION
    var currentChainId: Int = Int.InvalidId
    var currentSymbol: String = String.Empty

    val currentAccounts: List<Account>
        get() = getValidAccounts(currentChainId)

    private val _createAccountLiveData = MutableLiveData<Event<Unit>>()
    val createAccountLiveData: LiveData<Event<Unit>> get() = _createAccountLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> = _errorLiveData

    fun getValidAccounts(chainId: Int) = accountManager.getAllActiveAccounts(chainId).apply { currentChainId = chainId }

    fun getValidAccountsAndLimit(chainId: Int = currentChainId) =
        accountManager.getNumberOfAccountsToUse() to getValidAccounts(chainId)

    fun getCurrentCheckSumAddress() =
        getValidAccounts(currentChainId)[spinnerPosition].let {
            accountManager.toChecksumAddress(
                it.address,
                it.chainId
            )
        }

    fun createNewAccount() {
        launchDisposable {
            accountManager.createOrUnhideAccount(NetworkManager.getNetwork(currentChainId))
                .flatMapCompletable { walletActionsRepository.saveWalletActions(listOf(getWalletAction(it))) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _createAccountLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun setAccountSpinnerDefaultPosition() {
        spinnerPosition = DEFAULT_CRYPTO_POSITION
    }

    private fun getWalletAction(accountName: String) =
        WalletAction(
            WalletActionType.ACCOUNT,
            WalletActionStatus.ADDED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, accountName))
        )

    companion object {
        const val DEFAULT_CRYPTO_POSITION = 0
        private const val ETH_RAMP_SYMBOL = "ETH"
        private const val DAI_RAMP_SYMBOL = "DAI"
        private const val XDAI_RAMP_SYMBOL = "xDAI"
        private const val USDC_RAMP_SYMBOL = "USDC"
        private const val MATIC_RAMP_SYMBOL = "MATIC"
        private const val USDT_RAMP_SYMBOL = "USDT"
        private const val MATIC_USDC_RAMP_SYMBOL = "MATIC_USDC"
        private const val MATIC_DAI_RAMP_SYMBOL = "MATIC_DAI"
        private const val BSC_RAMP_SYMBOL = "BSC_BNB"
        private fun getNetworkName(chainId: Int): String = NetworkManager.getNetwork(chainId).name

        val rampCrypto
            get() = listOf(
                RampCrypto(
                    ChainId.ETH_MAIN,
                    ETH_RAMP_SYMBOL,
                    R.drawable.ic_ethereum_token,
                    getNetworkName(ChainId.ETH_MAIN),
                    isSelected = true
                ),
                RampCrypto(
                    ChainId.MATIC,
                    MATIC_RAMP_SYMBOL,
                    R.drawable.ic_polygon_matic_token,
                    getNetworkName(ChainId.MATIC)
                ),
                RampCrypto(
                    ChainId.XDAI,
                    XDAI_RAMP_SYMBOL,
                    R.drawable.ic_gnosis_chain_token,
                    getNetworkName(ChainId.XDAI)
                ),
                RampCrypto(ChainId.BSC, BSC_RAMP_SYMBOL, R.drawable.ic_bsc_token, getNetworkName(ChainId.BSC)),

                RampCrypto(
                    ChainId.ETH_MAIN,
                    DAI_RAMP_SYMBOL,
                    R.drawable.ic_dai_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(
                    ChainId.ETH_MAIN,
                    USDC_RAMP_SYMBOL,
                    R.drawable.ic_usdc_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(
                    ChainId.ETH_MAIN,
                    USDT_RAMP_SYMBOL,
                    R.drawable.ic_usdt_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(ChainId.MATIC, ETH_RAMP_SYMBOL, R.drawable.ic_ethereum_token, getNetworkName(ChainId.MATIC)),

                RampCrypto(
                    ChainId.MATIC,
                    MATIC_DAI_RAMP_SYMBOL,
                    R.drawable.ic_dai_token,
                    getNetworkName(ChainId.MATIC)
                ),
                RampCrypto(
                    ChainId.MATIC,
                    MATIC_USDC_RAMP_SYMBOL,
                    R.drawable.ic_usdc_token,
                    getNetworkName(ChainId.MATIC)
                )
            )
    }
}