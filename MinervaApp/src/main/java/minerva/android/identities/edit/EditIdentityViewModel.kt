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

    private var identityName: String = String.Empty

    private val _editIdentityLiveData = MutableLiveData<Event<Identity>>()
    val editIdentityLiveData: LiveData<Event<Identity>> get() = _editIdentityLiveData

    private val _saveCompletedLiveData = MutableLiveData<Event<Int>>()
    val saveCompletedLiveData: LiveData<Event<Int>> get() = _saveCompletedLiveData

    private val _saveWalletActionLiveData = MutableLiveData<Event<Unit>>()
    val saveWalletActionLiveData: LiveData<Event<Unit>> get() = _saveWalletActionLiveData

    private val _saveErrorLiveData = MutableLiveData<Event<Throwable>>()
    val saveErrorLiveData: LiveData<Event<Throwable>> get() = _saveErrorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun loadIdentity(position: Int, defaultName: String) {
        _editIdentityLiveData.value = Event(walletManager.loadIdentity(position, defaultName))
    }

    fun saveIdentity(identity: Identity) {
        _loadingLiveData.value = Event(true)
        identityName = identity.name
        launchDisposable {
            walletManager.saveIdentity(identity)
                .subscribeBy(
                    onComplete = {
                        _saveCompletedLiveData.value = Event(identity.index)
                    },
                    onError = {
                        _saveErrorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }

    fun saveWalletAction(status: Int) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(getWalletAction(status), walletManager.masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _saveWalletActionLiveData.value = Event(Unit)
                    }, onError = {
                        _saveErrorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }

    private fun getWalletAction(status: Int) =
        WalletAction(WalletActionType.IDENTITY, status, DateUtils.timestamp, hashMapOf(Pair(WalletActionFields.INDENTITY_NAME, identityName)))
}