package minerva.android.values.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.CryptoUtils
import minerva.android.walletmanager.utils.DateUtils

class NewValueViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
    BaseViewModel() {

    private var valueName: String = String.Empty

    private val _createValueLiveData = MutableLiveData<Event<Unit>>()
    val createValueLiveData: LiveData<Event<Unit>> get() = _createValueLiveData

    private val _saveErrorLiveData = MutableLiveData<Event<Throwable>>()
    val saveErrorLiveData: LiveData<Event<Throwable>> get() = _saveErrorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createNewValue(network: Network, position: Int) {
        valueName = CryptoUtils.prepareName(network, position)
        launchDisposable {
            walletManager.createValue(network, valueName)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(getWalletAction(), walletManager.masterSeed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _createValueLiveData.value = Event(Unit) },
                    onError = {
                        //Panic Button. Uncomment code below to save manually - not recommended
                        _saveErrorLiveData.value = Event(it)
//                        _createValueLiveData.value = Event(Unit)
                    }
                )
        }
    }

    private fun getWalletAction() =
        WalletAction(
            WalletActionType.VALUE,
            WalletActionStatus.ADDED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.VALUE_NAME, valueName))
        )
}