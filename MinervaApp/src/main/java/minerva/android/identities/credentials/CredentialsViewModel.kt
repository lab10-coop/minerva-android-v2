package minerva.android.identities.credentials

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.walletActions.WalletActionsRepository

open class CredentialsViewModel(
    private val identityManager: IdentityManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = identityManager.walletConfigLiveData

    open val errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    open val errorLiveData: LiveData<Event<Throwable>> get() = errorMutableLiveData

    private val _removeCredentialLiveData = MutableLiveData<Event<Any>>()
    val removeCredentialLiveData: LiveData<Event<Any>> get() = _removeCredentialLiveData

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

    fun saveWalletAction(walletAction: WalletAction): Completable =
        walletActionsRepository.saveWalletActions(listOf(walletAction))

    fun getRemovedItemWalletAction(type: Int, name: String, field: String) =
        WalletAction(
            type,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(field, name))
        )

    fun getLoggedIdentityName(loggedInIdentityDid: String): String = identityManager.loadIdentityByDID(loggedInIdentityDid).name

}