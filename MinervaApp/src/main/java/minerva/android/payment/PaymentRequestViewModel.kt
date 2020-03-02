package minerva.android.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.operators.completable.CompletableDefer
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import minerva.android.kotlinUtils.InvalidVersion
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Payment
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.AUTHORISED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.SIGNED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DateUtils

class PaymentRequestViewModel(private val walletManager: WalletManager, private val walletActionsRepository: WalletActionsRepository) :
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
        services?.find { service -> service.name == M27 } != null

    fun connectToService() {
        var walletConfig = WalletConfig(Int.InvalidVersion)
        launchDisposable {
            walletManager.saveService(Service(ServiceType.M27, payment.shortName, DateUtils.getLastUsedFormatted()))
                .map { walletConfig = it }
                .flatMapCompletable { walletActionsRepository.saveWalletActions(getWalletAction(AUTHORISED), walletManager.masterKey) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        _newServiceMutableLiveData.value = Event(Unit)
                        updateValidWalletConfig(walletConfig)
                    },
                    onError = {
                        _errorMutableLiveData.value = Event(it)
                    }
                )
        }
    }

    private fun updateValidWalletConfig(walletConfig: WalletConfig) {
        if (walletConfig.version != Int.InvalidVersion)
            walletManager.walletConfigMutableLiveData.value = walletConfig
    }

    fun confirmTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            val jwtToken = walletManager.createJwtToken(encodedData(), walletManager.masterKey.privateKey)
            withContext(Dispatchers.Main) {
                launchDisposable { saveSignedWalletAction(jwtToken) }
            }
        }
    }

    private fun saveSignedWalletAction(jwtToken: String): Disposable =
        walletActionsRepository.saveWalletActions(getWalletAction(SIGNED), walletManager.masterKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { _confirmPaymentMutableLiveData.value = Event(jwtToken) },
                onError = { _errorMutableLiveData.value = Event(it) }
            )

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

    fun isMasterKeyAvailable() = walletManager.isMasterKeyAvailable()

    companion object {
        const val M27 = "M27"
    }
}