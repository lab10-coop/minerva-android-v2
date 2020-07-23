package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class IdentitiesViewModel(
    private val identityManager: IdentityManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = identityManager.walletConfigLiveData

    private val _identityRemovedLiveData = MutableLiveData<Event<Unit>>()
    val identityRemovedLiveData: LiveData<Event<Unit>> get() = _identityRemovedLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun removeIdentity(identity: Identity) {
        launchDisposable {
            identityManager.removeIdentity(identity)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(getRemovedIdentityWalletAction(identity.name)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _identityRemovedLiveData.value = Event(Unit) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    private fun getRemovedIdentityWalletAction(name: String) =
        WalletAction(
            WalletActionType.IDENTITY,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, name))
        )
}