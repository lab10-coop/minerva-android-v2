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
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ServicesViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    val servicesLiveData: LiveData<List<Service>> =
        Transformations.map(serviceManager.walletConfigLiveData) {
            setDappSessionsFlowable(it.services)
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

    internal fun setDappSessionsFlowable(services: List<MinervaPrimitive>) {
        launchDisposable {
            walletConnectRepository.getSessionsFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _dappSessionsLiveData.value = services.mergeWithoutDuplicates(it) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun removeSession(dapp: DappSession) {
        launchDisposable {
            //toto tutaj powinien byc serwis, gdzie jest robione killSession, a potem usuwanie z db juz w serwisie
            walletConnectRepository.deleteDappSession(dapp.peerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { Timber.e(it) })
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