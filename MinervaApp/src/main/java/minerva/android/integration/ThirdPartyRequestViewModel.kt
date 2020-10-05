package minerva.android.integration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SIGNED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.mappers.GATEWAY
import minerva.android.walletmanager.model.state.VCRequestState
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class ThirdPartyRequestViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val masterSeedRepository: MasterSeedRepository
) : BaseViewModel() {

    lateinit var payment: Payment
    lateinit var credentialRequest: Pair<Credential, CredentialRequest>

    private val _showConnectionRequestLiveData = MutableLiveData<Event<VCRequestState<Pair<Credential, CredentialRequest>>>>()
    val showServiceConnectionRequestLiveData: LiveData<Event<VCRequestState<Pair<Credential, CredentialRequest>>>> get() = _showConnectionRequestLiveData

    private val _showPaymentConfirmationLiveData = MutableLiveData<Event<Unit>>()
    val showPaymentConfirmationLiveData: LiveData<Event<Unit>> get() = _showPaymentConfirmationLiveData

    private val _addedNewServiceLiveData = MutableLiveData<Event<Unit>>()
    val addedNewServiceLiveData: LiveData<Event<Unit>> get() = _addedNewServiceLiveData

    private val _confirmPaymentLiveData = MutableLiveData<Event<String>>()
    val confirmPaymentLiveData: LiveData<Event<String>> get() = _confirmPaymentLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    val connectionReason: String?
        get() = credentialRequest.second.credentialRequirements?.reason

    val requestedData: String
        get() = credentialRequest.first.name

    private val serviceName: String
        get() = credentialRequest.second.service.name

    private val serviceIconUrl: String
        get() = GATEWAY + credentialRequest.second.service.iconUrl.url

    fun decodeJwtToken(token: String?) {
        token?.let {
            launchDisposable {
                serviceManager.decodeThirdPartyRequestToken(token)
                    .doOnSuccess { if (it is VCRequestState.Found) credentialRequest = it.data }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { _showConnectionRequestLiveData.value = Event(it) },
                        onError = { _errorLiveData.value = Event(it) }
                    )
            }
        }.orElse {
            _errorLiveData.value = Event(Throwable())
        }
    }

    //todo should be handle when services request is ready on m27 app
//    private fun handleDecodeResult(payment: Payment, services: List<Service>?) {
//        this.payment = payment
//        checkIfServiceIsAlreadyConnected(services, payment)
//        //todo check what kind of interaction is requested and display a proper fragment for that
//    }
//
//    private fun checkIfServiceIsAlreadyConnected(services: List<Service>?, payment: Payment) {
//        if (isM27Connected(services)) _showPaymentConfirmationLiveData.value = Event(Unit)
//        else _showConnectionRequestLiveData.value = Event(payment.serviceName)
//    }
//
//    private fun isM27Connected(services: List<Service>?) =
//        services?.find { service -> service.name == M27_NAME } != null

    fun connectToService() {
        launchDisposable {
            serviceManager.saveService(Service(ServiceType.OAMTC, serviceName, DateUtils.timestamp, iconUrl = serviceIconUrl))
                .observeOn(Schedulers.io())
//                .andThen(walletActionsRepository.saveWalletActions(getWalletAction(AUTHORISED))) //todo add and when handling activity logs
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _addedNewServiceLiveData.value = Event(Unit) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun confirmTransaction() {
        launchDisposable {
            serviceManager.createJwtToken(encodedData())
                .flatMap { walletActionsRepository.saveWalletActions(getWalletAction(SIGNED)).toSingleDefault(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _confirmPaymentLiveData.value = Event(it) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    private fun encodedData() =
        mapOf(
            PaymentRequest.AMOUNT to payment.amount,
            PaymentRequest.IBAN to payment.iban,
            PaymentRequest.SERVICE_NAME to payment.serviceName,
            PaymentRequest.RECIPIENT to payment.recipient
        )

    private fun getWalletAction(status: Int): WalletAction {
        return WalletAction(
            WalletActionType.SERVICE,
            status,
            DateUtils.timestamp,
            hashMapOf(Pair(WalletActionFields.SERVICE_NAME, payment.shortName))
        )
    }

    fun isMasterSeedAvailable() = masterSeedRepository.isMasterSeedAvailable()
    fun initWalletConfig() = masterSeedRepository.initWalletConfig()
}