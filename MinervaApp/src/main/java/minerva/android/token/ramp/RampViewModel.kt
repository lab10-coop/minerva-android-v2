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
        private fun getNetworkName(chainId: Int): String = NetworkManager.getNetwork(chainId).name

        val rampCrypto
            get() = listOf(
                RampCrypto(
                    ChainId.ETH_MAIN,
                    "ETH",
                    "ETH_ETH",
                    R.drawable.ic_ethereum_token,
                    getNetworkName(ChainId.ETH_MAIN),
                    isSelected = true
                ),
                RampCrypto(
                    ChainId.MATIC,
                    "MATIC",
                    "MATIC_MATIC",
                    R.drawable.ic_polygon_matic_token,
                    getNetworkName(ChainId.MATIC)
                ),
                RampCrypto(
                    ChainId.XDAI,
                    "xDAI",
                    "XDAI_XDAI",
                    R.drawable.ic_gnosis_chain_token,
                    getNetworkName(ChainId.XDAI)
                ),
                RampCrypto(
                    ChainId.BSC,
                    "BSC",
                    "BSC_BNB",
                    R.drawable.ic_bsc_token,
                    getNetworkName(ChainId.BSC)
                ),

                RampCrypto(
                    ChainId.AVA_C,
                    "AVAX",
                    "AVAX_AVAX",
                    R.drawable.ic_avalanche,
                    getNetworkName(ChainId.AVA_C)
                ),
                RampCrypto(
                    ChainId.CELO,
                    "CELO",
                    "CELO_CELO",
                    R.drawable.ic_celo_coin,
                    getNetworkName(ChainId.CELO)
                ),
                RampCrypto(
                    ChainId.ARB_ONE,
                    "ETH",
                    "ARBITRUM_ETH",
                    R.drawable.ic_ethereum_l2,
                    getNetworkName(ChainId.ARB_ONE)
                ),
                RampCrypto(
                    ChainId.OPT,
                    "ETH",
                    "OPTIMISM_ETH",
                    R.drawable.ic_ethereum_l2,
                    getNetworkName(ChainId.OPT)
                ),

                RampCrypto(
                    ChainId.ETH_MAIN,
                    "DAI",
                    "ETH_DAI",
                    R.drawable.ic_dai_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(
                    ChainId.ETH_MAIN,
                    "USDT",
                    "ETH_USDT",
                    R.drawable.ic_usdt_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(
                    ChainId.ETH_MAIN,
                    "USDC",
                    "ETH_USDC",
                    R.drawable.ic_usdc_token,
                    getNetworkName(ChainId.ETH_MAIN)
                ),
                RampCrypto(
                    ChainId.MATIC,
                    "DAI",
                    "MATIC_DAI",
                    R.drawable.ic_dai_token,
                    getNetworkName(ChainId.MATIC)
                ),

                RampCrypto(
                    ChainId.MATIC,
                    "USDC",
                    "MATIC_USDC",
                    R.drawable.ic_usdc_token,
                    getNetworkName(ChainId.MATIC)
                ),
                RampCrypto(
                    ChainId.MATIC,
                    "ETH",
                    "MATIC_ETH",
                    R.drawable.ic_ethereum_token,
                    getNetworkName(ChainId.MATIC)
                ),
                RampCrypto(
                    ChainId.OPT,
                    "DAI",
                    "OPTIMISM_DAI",
                    R.drawable.ic_dai_token,
                    getNetworkName(ChainId.OPT)
                ),
                RampCrypto(
                    ChainId.CELO,
                    "cUSD",
                    "CELO_CUSD",
                    R.drawable.ic_celo_dollar,
                    getNetworkName(ChainId.CELO)
                ),

                RampCrypto(
                    ChainId.CELO,
                    "cEUR",
                    "CELO_CEUR",
                    R.drawable.ic_celo_dollar,
                    getNetworkName(ChainId.CELO)
                ),
                RampCrypto(
                    ChainId.CELO,
                    "cREAL",
                    "CELO_CREAL",
                    R.drawable.ic_celo_creal,
                    getNetworkName(ChainId.CELO)
                )
            )
    }
}
