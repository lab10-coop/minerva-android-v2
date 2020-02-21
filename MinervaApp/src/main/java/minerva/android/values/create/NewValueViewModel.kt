package minerva.android.values.create

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
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.utils.DateUtils

class NewValueViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
    BaseViewModel() {

    private var valueName: String = String.Empty

    private val _saveCompletedLiveData = MutableLiveData<Event<Int>>()
    val saveCompletedLiveData: LiveData<Event<Int>> get() = _saveCompletedLiveData

    private val _saveWalletActionLiveData = MutableLiveData<Event<Unit>>()
    val saveWalletActionLiveData: LiveData<Event<Unit>> get() = _saveWalletActionLiveData

    private val _saveErrorLiveData = MutableLiveData<Event<Throwable>>()
    val saveErrorLiveData: LiveData<Event<Throwable>> get() = _saveErrorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createNewValue(network: Network, position: Int) {
        _loadingLiveData.value = Event(true)
        valueName = prepareName(network, position)
        launchDisposable {
            walletManager.createValue(network, valueName)
                .subscribeBy(
                    onComplete = { _saveCompletedLiveData.value = Event(position) },
                    onError = { _saveErrorLiveData.value = Event(Throwable("Unexpected creating value error: ${it.message}")) }
                )
        }
    }

    fun saveWalletAction() {
        launchDisposable {
            walletActionsRepository.saveWalletActions(getWalletAction(), walletManager.masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _saveWalletActionLiveData.value = Event(Unit)
                    }, onError = {
                        _saveErrorLiveData.value = Event(it)
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

    private fun prepareName(network: Network, position: Int) = String.format(VALUE_NAME_PATTERN, position, network.full)

    companion object {
        private const val VALUE_NAME_PATTERN = "#%d %s"
    }
}