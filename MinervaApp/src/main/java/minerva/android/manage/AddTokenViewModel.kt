package minerva.android.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import timber.log.Timber

class AddTokenViewModel(
    private val transactionRepository: TransactionRepository,
    private val smartContractRepository: SmartContractRepository,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    private var privateKey = String.Empty
    private var network = String.Empty

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _addressDetailsLiveData = MutableLiveData<Token>()
    val addressDetailsLiveData: LiveData<Token> get() = _addressDetailsLiveData

    private val _tokenAddedLiveData = MutableLiveData<Event<Unit>>()
    val tokenAddedLiveData: LiveData<Event<Unit>> get() = _tokenAddedLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun initViewModel(privateKey: String, network: String) {
        this.privateKey = privateKey
        this.network = network
    }

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address)

    fun getTokenDetails(address: String) =
        launchDisposable {
            smartContractRepository.getERC20TokenDetails(privateKey, network, address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = { _addressDetailsLiveData.value = it },
                    onError = {
                        Timber.e("Checking Asset details error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }

    fun addToken(token: Token) =
        launchDisposable {
            tokenManager.saveToken(network, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _tokenAddedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Error during downloading token data: ${it.message}")
                        _errorLiveData.value = Event(it)
                    })
        }
}