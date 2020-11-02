package minerva.android.identities.myIdentities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.identities.credentials.CredentialsViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.IDENTITY
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MyIdentitiesViewModel(
    private val identityManager: IdentityManager,
    walletActionsRepository: WalletActionsRepository
) : CredentialsViewModel(identityManager, walletActionsRepository) {

    private val _identityRemovedLiveData = MutableLiveData<Event<Unit>>()
    val identityRemovedLiveData: LiveData<Event<Unit>> get() = _identityRemovedLiveData

    fun removeIdentity(identity: Identity) {
        launchDisposable {
            identityManager.removeIdentity(identity)
                .andThen(saveWalletAction(getRemovedItemWalletAction(IDENTITY, identity.name, IDENTITY_NAME)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _identityRemovedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e(it)
                        errorMutableLiveData.value = Event(it)
                    }
                )
        }
    }
}