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
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.walletconnect.DappSessionV2
import minerva.android.walletmanager.model.walletconnect.Pairing
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ServicesViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val walletConnectRepository: WalletConnectRepository
) : BaseViewModel() {

    private val _servicesLiveData: MutableLiveData<List<Service>> =
        Transformations.map(serviceManager.walletConfigLiveData) {
            it.peekContent().services.apply {
                setDappSessionsFlowable(this)
            }
        } as MutableLiveData<List<Service>>
    val servicesLiveData: LiveData<List<Service>> = _servicesLiveData

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
            walletConnectRepository.getSessionsAndPairingsFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { _dappSessionsLiveData.value = services.mergeWithoutDuplicates(it) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun removeSession(dapp: DappSession) {
        when (dapp) {
            is DappSessionV1 -> launchDisposable {
                walletConnectRepository.killSessionByPeerId(dapp.peerId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onError = { Timber.e(it) })
            }
            is DappSessionV2 -> {
                walletConnectRepository.killSessionByTopic(dapp.topic)
                // Update servicesLiveData
                // todo: this seems to be a bit hacky, fix this.
                val updatedServices = serviceManager.walletConfigLiveData.value?.peekContent()?.services?.apply {
                    setDappSessionsFlowable(this)
                } ?: emptyList()
                updatedServices.let { _servicesLiveData.postValue(it) }
            }
        }

    }

    fun removePairing(dapp: MinervaPrimitive) {
        when (dapp) {
            is Pairing -> walletConnectRepository.killPairingByTopic(dapp.topic)
            is DappSessionV2 -> {
                // the order of these two must be like this.
                walletConnectRepository.killPairingBySessionTopic(dapp.topic)
                walletConnectRepository.killSessionByTopic(dapp.topic)
            }
        }
        // Update servicesLiveData
        // todo: this seems to be a bit hacky, fix this.
        val updatedServices = serviceManager.walletConfigLiveData.value?.peekContent()?.services?.apply {
            setDappSessionsFlowable(this)
        } ?: emptyList()
        updatedServices.let { _servicesLiveData.postValue(it) }
    }

    private fun getWalletAction(name: String): WalletAction =
        WalletAction(
            WalletActionType.SERVICE,
            WalletActionStatus.REMOVED,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.SERVICE_NAME, name))
        )
}