package minerva.android.integration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
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
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.CREDENTIAL_NAME
import minerva.android.walletmanager.model.defs.WalletActionFields.Companion.SERVICE_NAME
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ACCEPTED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.REJECTED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SENT
import minerva.android.walletmanager.model.defs.WalletActionType.Companion.SERVICE
import minerva.android.walletmanager.model.mappers.GATEWAY
import minerva.android.walletmanager.model.state.ConnectionRequest
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ThirdPartyRequestViewModel(
    private val serviceManager: ServiceManager,
    private val masterSeedRepository: MasterSeedRepository,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    lateinit var payment: Payment
    lateinit var credentialRequest: Pair<Credential, CredentialRequest>

    private val _showConnectionRequestLiveData = MutableLiveData<Event<ConnectionRequest<Pair<Credential, CredentialRequest>>>>()
    val showServiceConnectionRequestLiveData: LiveData<Event<ConnectionRequest<Pair<Credential, CredentialRequest>>>> get() = _showConnectionRequestLiveData

    private val _showPaymentConfirmationLiveData = MutableLiveData<Event<Unit>>()
    val showPaymentConfirmationLiveData: LiveData<Event<Unit>> get() = _showPaymentConfirmationLiveData

    private val _addedNewServiceLiveData = MutableLiveData<Event<Pair<Credential, CredentialRequest>>>()
    val addedNewServiceLiveData: LiveData<Event<Pair<Credential, CredentialRequest>>> get() = _addedNewServiceLiveData

    private val _confirmPaymentLiveData = MutableLiveData<Event<String>>()
    val confirmPaymentLiveData: LiveData<Event<String>> get() = _confirmPaymentLiveData

    private val _onDenyConnectionSuccessLiveData = MutableLiveData<Event<Unit>>()
    val onDenyConnectionSuccessLiveData: LiveData<Event<Unit>> get() = _onDenyConnectionSuccessLiveData

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

    private val issuer: String
        get() = credentialRequest.second.service.issuer

    private val serviceIconUrl: String
        get() = GATEWAY + credentialRequest.second.service.iconUrl.url

    fun decodeJwtToken(token: String?) {
        token?.let {
            launchDisposable {
                serviceManager.decodeThirdPartyRequestToken(token)
                    .observeOn(Schedulers.io())
                    .flatMap {
                        when (it) {
                            is ConnectionRequest.ServiceConnected -> saveWalletActions(it)
                            else -> Single.just(it)
                        }
                    }
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

    fun connectToService() {
        launchDisposable {
            serviceManager.saveService(Service(issuer, serviceName, DateUtils.timestamp, iconUrl = serviceIconUrl))
                .observeOn(Schedulers.io())
                .andThen(walletActionsRepository.saveWalletActions(saveServiceWalletActions))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _addedNewServiceLiveData.value = Event(credentialRequest) },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }

    fun saveDenyConnectionWalletAction() {
        launchDisposable {
            walletActionsRepository.saveWalletActions(listOf(rejectedConnectionWalletAction))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _onDenyConnectionSuccessLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Service wallet action error $it")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    private val rejectedConnectionWalletAction
        get() = getWalletAction(REJECTED, hashMapOf(SERVICE_NAME to credentialRequest.second.service.name))

    private val saveServiceWalletActions
        get() = listOf(
            getWalletAction(SENT, sentCredentialMap),
            getWalletAction(ADDED, hashMapOf(SERVICE_NAME to serviceName)),
            getWalletAction(ACCEPTED, hashMapOf(SERVICE_NAME to serviceName))
        )

    private val sentCredentialMap
        get() = hashMapOf(SERVICE_NAME to credentialRequest.second.service.name, CREDENTIAL_NAME to credentialRequest.first.name)

    private fun saveWalletActions(it: ConnectionRequest.ServiceConnected<Pair<Credential, CredentialRequest>>) =
        walletActionsRepository.saveWalletActions(
            listOf(
                getWalletAction(SENT, sentCredentialMap(it)),
                getWalletAction(WalletActionStatus.BACKGROUND_ADDED, hashMapOf(SERVICE_NAME to it.data.second.service.name))
            )
        ).toSingleDefault(it)

    private fun sentCredentialMap(it: ConnectionRequest.ServiceConnected<Pair<Credential, CredentialRequest>>) =
        hashMapOf(SERVICE_NAME to it.data.second.service.name, CREDENTIAL_NAME to it.data.first.name)

    private fun getWalletAction(status: Int, payload: HashMap<String, String>): WalletAction =
        WalletAction(SERVICE, status, DateUtils.timestamp, payload)

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

    fun confirmTransaction() {
        launchDisposable {
            serviceManager.createJwtToken(encodedData())
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

    fun isMasterSeedAvailable() = masterSeedRepository.isMasterSeedAvailable()
    fun initWalletConfig() = masterSeedRepository.initWalletConfig()
}