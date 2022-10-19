package minerva.android.accounts.create

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class NewAccountViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    var selectedNetworkPosition: Int = DEFAULT_NETWORK_POSITION
    var selectedNetworkChainId: Int? = null

    init {
        shouldAddAccount()
    }

    val unusedAddresses
        get() = selectedNetworkChainId?.let { chainId ->
            accountManager.getAllFreeAccountForNetwork(chainId)
                .map { w -> AddressWrapper(w.index, accountManager.toChecksumAddress(w.address), w.status)  }
        } ?: emptyList()

    private val _createAccountLiveData = MutableLiveData<Event<Unit>>()
    val createAccountLiveData: LiveData<Event<Unit>> get() = _createAccountLiveData

    private val _refreshAddressesLiveData = MutableLiveData<Event<Unit>>()
    val refreshAddressesLiveData: LiveData<Event<Unit>> get() = _refreshAddressesLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> = _errorLiveData

    fun connectAccountToNetwork(index: Int, network: Network) {
        launchDisposable {
            accountManager.connectAccountToNetwork(index, network)
                .flatMapCompletable { walletActionsRepository.saveWalletActions(listOf(getWalletAction(it))) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnError { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _createAccountLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    @VisibleForTesting
    fun shouldAddAccount() {
        val accounts = accountManager.getAllAccountsForSelectedNetworksType().filter { account -> !account.isDeleted }.distinctBy { account -> account.id }.size
        if (accounts < FREE_ACCOUNT_MAX_NUMBER) {
            launchDisposable {
                accountManager.createEmptyAccounts(FREE_ACCOUNT_MAX_NUMBER - accounts)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onComplete = {
                            _refreshAddressesLiveData.value = Event(Unit)
                        }
                    )
            }
        }
    }

    private fun getWalletAction(accountName: String) =
        WalletAction(
            WalletActionType.ACCOUNT,
            WalletActionStatus.ADDED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, accountName))
        )

    val areMainNetsEnabled: Boolean
        get() = accountManager.areMainNetworksEnabled

    companion object {
        private const val FREE_ACCOUNT_MAX_NUMBER = 5
        private const val DEFAULT_NETWORK_POSITION = 0
    }
}