package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.CREDENTIAL_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.CREDENTIAL
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.IDENTITY
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

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
                .doOnComplete { saveWalletAction(getRemovedItemWalletAction(IDENTITY, identity.name, IDENTITY_NAME)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _identityRemovedLiveData.value = Event(Unit) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun removeCredential(credential: Credential) {
        launchDisposable {
            identityManager.removeBindedCredentialFromIdentity(credential)
                .doOnComplete { saveWalletAction(getRemovedItemWalletAction(CREDENTIAL, credential.name, CREDENTIAL_NAME)) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { _errorLiveData.value = Event(it) })
        }
    }

    private fun saveWalletAction(walletAction: WalletAction) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(walletAction)
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onComplete = { Timber.d("Removed item wallet action saved successfully.") },
                    onError = { Timber.e("Removed item wallet action error: $it") }
                )
        }
    }

    private fun getRemovedItemWalletAction(type: Int, name: String, field: String) =
        WalletAction(
            type,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(field, name))
        )
}