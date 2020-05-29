package minerva.android.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.Payment
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.AUTHORISED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SIGNED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.storage.ServiceName.Companion.M27_NAME
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class PaymentRequestViewModel(private val walletManager: WalletManager, private val repository: WalletActionsRepository) :
    BaseViewModel() {

    lateinit var payment: Payment

    private val _showConnectionRequestMutableLiveData = MutableLiveData<Event<String?>>()
    val showConnectionRequestLiveData: LiveData<Event<String?>> get() = _showConnectionRequestMutableLiveData

    private val _showPaymentConfirmationMutableLiveData = MutableLiveData<Event<Unit>>()
    val showPaymentConfirmationLiveData: LiveData<Event<Unit>> get() = _showPaymentConfirmationMutableLiveData

    private val _newServiceMutableLiveData = MutableLiveData<Event<Unit>>()
    val newServiceMutableLiveData: LiveData<Event<Unit>> get() = _newServiceMutableLiveData

    private val _confirmPaymentMutableLiveData = MutableLiveData<Event<String>>()
    val confirmPaymentLiveData: LiveData<Event<String>> get() = _confirmPaymentMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun decodeJwtToken(token: String?) {
        token?.let {
            launchDisposable {
                walletManager.decodePaymentRequestToken(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { handleDecodeResult(it.first, it.second) },
                        onError = { _errorMutableLiveData.value = Event(it) }
                    )
            }
        }.orElse {
            _errorMutableLiveData.value = Event(Throwable())
        }
    }

    private fun handleDecodeResult(payment: Payment, services: List<Service>?) {
        this.payment = payment
        checkIfServiceIsAlreadyConnected(services, payment)
    }

    private fun checkIfServiceIsAlreadyConnected(services: List<Service>?, payment: Payment) {
        if (isM27Connected(services)) _showPaymentConfirmationMutableLiveData.value = Event(Unit)
        else _showConnectionRequestMutableLiveData.value = Event(payment.serviceName)
    }

    private fun isM27Connected(services: List<Service>?) =
        services?.find { service -> service.name == M27_NAME } != null

    fun connectToService() {
        launchDisposable {
            walletManager.saveService(Service(ServiceType.M27, payment.shortName, DateUtils.getLastUsedFormatted()))
                .observeOn(Schedulers.io())
                .andThen(repository.saveWalletActions(getWalletAction(AUTHORISED), walletManager.masterSeed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _newServiceMutableLiveData.value = Event(Unit) },
                    onError = { _errorMutableLiveData.value = Event(it) }
                )
        }
    }

    fun confirmTransaction() {
        launchDisposable {
            walletManager.createJwtToken(encodedData(), walletManager.masterSeed.privateKey)
                .flatMap { repository.saveWalletActions(getWalletAction(SIGNED), walletManager.masterSeed).toSingleDefault(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _confirmPaymentMutableLiveData.value = Event(it) },
                    onError = { _errorMutableLiveData.value = Event(it) }
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

    fun isMasterSeedAvailable() = walletManager.isMasterSeedAvailable()
    fun initWalletConfig() = walletManager.initWalletConfig()
}