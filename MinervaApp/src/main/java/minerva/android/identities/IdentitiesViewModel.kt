package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

class IdentitiesViewModel(
    private val walletManager: WalletManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    private var identityName: String = String.Empty

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _removeIdentityLiveData = MutableLiveData<Event<Unit>>()
    val removeIdentityLiveData: LiveData<Event<Unit>> get() = _removeIdentityLiveData

    fun removeIdentity(identity: Identity) {
        identityName = identity.name
        launchDisposable {
            walletManager.removeIdentity(identity)
                .subscribeBy(
                    onComplete = { _removeIdentityLiveData.value = Event(Unit) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun saveRemoveIdentityWallet() {
        launchDisposable {
            walletActionsRepository.saveWalletActions(getRemovedIdentityWalletAction(), walletManager.masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { /*Handled in wallet manager */ },
                    onError = {
                        _errorLiveData.value = Event(Throwable(it))
                    }
                )
        }
    }

    private fun getRemovedIdentityWalletAction() =
        WalletAction(
            WalletActionType.IDENTITY,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, identityName))
        )
}