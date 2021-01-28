package minerva.android.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.walletconnect.DappSessionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class ServicesViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val dappSessionRepository: DappSessionRepository
) : BaseViewModel() {

    val walletConfigLiveData: LiveData<List<Service>> =
        Transformations.map(serviceManager.walletConfigLiveData) {
            setDappSessionsFlowable(it.services) //todo zeby tylko raz updatowac liste, moze wywal to i zrob tylko z pobieraniem z bz danych i dolaczaj services
            it.services
        }

    private val _serviceRemovedLiveData = MutableLiveData<Event<Unit>>()
    val serviceRemovedLiveData: LiveData<Event<Unit>> get() = _serviceRemovedLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _dappSessionsLiveData = MutableLiveData<List<MinervaPrimitive>>()
    val dappSessionsLiveData: LiveData<List<MinervaPrimitive>> get() = _dappSessionsLiveData

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

    fun setDappSessionsFlowable(services: List<MinervaPrimitive>) {
        launchDisposable {
            dappSessionRepository.getSessionsFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        _dappSessionsLiveData.value =
                            services.mergeWithoutDuplicates(it) //services.mergeWithoutDuplicates(it)
                    },
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