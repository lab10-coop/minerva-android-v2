package minerva.android.identities.credentials

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.kotlinUtils.DateUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

open class CredentialsViewModel(
    private val identityManager: IdentityManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = identityManager.walletConfigLiveData

    open val errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    open val errorLiveData: LiveData<Event<Throwable>> get() = errorMutableLiveData

    private val _removeCredentialMutableLiveData = MutableLiveData<Event<Any>>()
    val removeCredentialMutableLiveData: LiveData<Event<Any>> get() = _removeCredentialMutableLiveData

    fun removeCredential(credential: Credential) {
        launchDisposable {
            identityManager.removeBindedCredentialFromIdentity(credential)
                .doOnComplete {
                    saveWalletAction(
                        getRemovedItemWalletAction(
                            WalletActionType.CREDENTIAL, credential.name,
                            WalletActionFields.CREDENTIAL_NAME
                        )
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _removeCredentialMutableLiveData.value = Event(Any()) },
                    onError = { errorMutableLiveData.value = Event(it) })
        }
    }

    fun saveWalletAction(walletAction: WalletAction) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(walletAction)
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onComplete = { Timber.d("Removed item wallet action saved successfully.") },
                    onError = { Timber.e("Removed item wallet action error: $it") }
                )
        }
    }

    fun getRemovedItemWalletAction(type: Int, name: String, field: String) =
        WalletAction(
            type,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(field, name))
        )

}