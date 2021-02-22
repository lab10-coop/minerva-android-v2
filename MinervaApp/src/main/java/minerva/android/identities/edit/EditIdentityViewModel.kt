package minerva.android.identities.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class EditIdentityViewModel(
    private val identityManager: IdentityManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    private val _editIdentityLiveData = MutableLiveData<Event<Identity>>()
    val editIdentityLiveData: LiveData<Event<Identity>> get() = _editIdentityLiveData

    private val _saveCompletedLiveData = MutableLiveData<Event<Identity>>()
    val saveCompletedLiveData: LiveData<Event<Identity>> get() = _saveCompletedLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData


    fun loadIdentity(position: Int, defaultName: String) {
        _editIdentityLiveData.value = Event(identityManager.loadIdentity(position, defaultName))
    }

    fun saveIdentity(identity: Identity, status: Int) {
        launchDisposable {
            identityManager.saveIdentity(identity)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(listOf(getWalletAction(status, identity.name))))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _saveCompletedLiveData.value = Event(identity) },
                    onError = {
                        Timber.e(it)
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun getWalletAction(status: Int, name: String) =
        WalletAction(
            WalletActionType.IDENTITY,
            status,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, name))
        )
}