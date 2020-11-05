package minerva.android.accounts.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.CryptoUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class NewAccountViewModel(
    private val accountManager: AccountManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    private var accountName: String = String.Empty

    private val _createAccountLiveData = MutableLiveData<Event<Unit>>()
    val createAccountLiveData: LiveData<Event<Unit>> get() = _createAccountLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> = _errorLiveData

    fun createNewAccount(network: Network, position: Int) {
        accountName = CryptoUtils.prepareName(network, position)
        launchDisposable {
            accountManager.createAccount(network, accountName)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(listOf(getWalletAction())))
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

    private fun getWalletAction() =
        WalletAction(
            WalletActionType.ACCOUNT,
            WalletActionStatus.ADDED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.ACCOUNT_NAME, accountName))
        )
}