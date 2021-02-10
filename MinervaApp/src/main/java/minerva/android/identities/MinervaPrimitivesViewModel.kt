package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.IDENTITY_NAME
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.IDENTITY
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class MinervaPrimitivesViewModel(
    private val identityManager: IdentityManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = identityManager.walletConfigLiveData

    private val errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = errorMutableLiveData

    private val _removeCredentialLiveData = MutableLiveData<Event<Any>>()
    val removeCredentialLiveData: LiveData<Event<Any>> get() = _removeCredentialLiveData

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

    fun removeCredential(credential: Credential) {
        launchDisposable {
            identityManager.removeBindedCredentialFromIdentity(credential)
                .andThen(
                    saveWalletAction(
                        getRemovedItemWalletAction(
                            WalletActionType.CREDENTIAL, credential.name,
                            WalletActionFields.CREDENTIAL_NAME
                        )
                    )
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _removeCredentialLiveData.value = Event(Any()) },
                    onError = { errorMutableLiveData.value = Event(it) })
        }
    }

    private fun saveWalletAction(walletAction: WalletAction): Completable =
        walletActionsRepository.saveWalletActions(listOf(walletAction))

    private fun getRemovedItemWalletAction(type: Int, name: String, field: String) =
        WalletAction(
            type,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(field, name))
        )

    fun getLoggedIdentityName(loggedInIdentityDid: String): String = identityManager.loadIdentityByDID(loggedInIdentityDid).name
}