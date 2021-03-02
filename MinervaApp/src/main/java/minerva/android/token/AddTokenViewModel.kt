package minerva.android.token

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import timber.log.Timber

class AddTokenViewModel(
    private val transactionRepository: TransactionRepository,
    private val smartContractRepository: SmartContractRepository,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    private var privateKey = String.Empty
    private var chainId = Int.InvalidId

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _addressDetailsLiveData = MutableLiveData<ERC20Token>()
    val addressDetailsLiveData: LiveData<ERC20Token> get() = _addressDetailsLiveData

    private val _tokenAddedLiveData = MutableLiveData<Event<Unit>>()
    val tokenAddedLiveData: LiveData<Event<Unit>> get() = _tokenAddedLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun initViewModel(privateKey: String, chainId: Int) {
        this.privateKey = privateKey
        this.chainId = chainId
    }

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address)

    fun getTokenDetails(address: String) =
        launchDisposable {
            smartContractRepository.getERC20TokenDetails(privateKey, chainId, address)
                .zipWith(tokenManager.getTokenIconURL(chainId, address),
                    BiFunction<ERC20Token, String, ERC20Token> { token, logoURI ->
                        token.apply {
                            this.logoURI = if(logoURI != String.Empty) logoURI
                            else null
                        }
                    }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = {
                        _addressDetailsLiveData.value = it
                    },
                    onError = {
                        Timber.e("Checking Asset details error: ${it.message}")
                        _errorLiveData.value = Event(it)
                    }
                )
        }

    fun addToken(token: ERC20Token) =
        launchDisposable {
            tokenManager.saveToken(chainId, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _tokenAddedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Error during downloading token data: ${it.message}")
                        _errorLiveData.value = Event(it)
                    })
        }

    fun getNetworkName() = NetworkManager.getNetwork(chainId).name
}