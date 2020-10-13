package minerva.android.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.kotlinUtils.DateUtils
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class ServicesViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = serviceManager.walletConfigLiveData

    private val _serviceRemovedLiveData = MutableLiveData<Event<Unit>>()
    val serviceRemovedLiveData: LiveData<Event<Unit>> get() = _serviceRemovedLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun removeService(issuer: String, name: String) {
        launchDisposable {
            serviceManager.removeService(issuer)
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(listOf(getWalletAction(name))))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _serviceRemovedLiveData.value = Event(Unit) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    private fun getWalletAction(name: String): WalletAction =
        WalletAction(
            WalletActionType.SERVICE,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.SERVICE_NAME, name))
        )
}