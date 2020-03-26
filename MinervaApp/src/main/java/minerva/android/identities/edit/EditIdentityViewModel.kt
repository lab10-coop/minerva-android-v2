package minerva.android.identities.edit

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
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

class EditIdentityViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
    BaseViewModel() {

    private val _editIdentityLiveData = MutableLiveData<Event<Identity>>()
    val editIdentityLiveData: LiveData<Event<Identity>> get() = _editIdentityLiveData

    private val _saveCompletedLiveData = MutableLiveData<Event<Unit>>()
    val saveCompletedLiveData: LiveData<Event<Unit>> get() = _saveCompletedLiveData

    private val _saveErrorLiveData = MutableLiveData<Event<Throwable>>()
    val saveErrorLiveData: LiveData<Event<Throwable>> get() = _saveErrorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun loadIdentity(position: Int, defaultName: String) {
        _editIdentityLiveData.value = Event(walletManager.loadIdentity(position, defaultName))
    }

    fun saveIdentity(identity: Identity, status: Int) {
        launchDisposable {
            walletManager.saveIdentity(identity)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(getWalletAction(status, identity.name), walletManager.masterKey))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _saveCompletedLiveData.value = Event(Unit) },
                    onError = {
                        //TODO Panic Button. Uncomment code below to save manually - not recommended
                        //_saveCompletedLiveData.value = Event(Unit)
                        _saveErrorLiveData.value = Event(it) //comment this if you want offline
                    }
                )
        }
    }

    private fun getWalletAction(status: Int, name: String) =
        WalletAction(WalletActionType.IDENTITY, status, DateUtils.timestamp, hashMapOf(Pair(WalletActionFields.IDENTITY_NAME, name)))
}