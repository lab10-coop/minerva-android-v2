package minerva.android.token

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.ContractAddressIsNotSupportedThrowable
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.utils.logger.Logger
import timber.log.Timber
import java.math.BigInteger

class AddTokenViewModel(
    private val transactionRepository: TransactionRepository,
    private val safeAccountRepository: SafeAccountRepository,
    private val tokenManager: TokenManager,
    private val logger: Logger
) : BaseViewModel() {

    private var privateKey = String.Empty
    private var accountAddress = String.Empty
    private var chainId = Int.InvalidId

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private var token: ERCToken? = null
        set(value) {
            field = value
            _tokenLiveData.value = value
        }

    private val _tokenLiveData = MutableLiveData<ERCToken>()
    val tokenLiveData: LiveData<ERCToken> get() = _tokenLiveData

    private val _isOwnerLiveData = MutableLiveData<Event<Result<Boolean>>>()
    val isOwnerLiveData: LiveData<Event<Result<Boolean>>> get() = _isOwnerLiveData

    private val _tokenAddedLiveData = MutableLiveData<Event<Unit>>()
    val tokenAddedLiveData: LiveData<Event<Unit>> get() = _tokenAddedLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun setAccountData(privateKey: String, chainId: Int, accountAddress: String) {
        this.privateKey = privateKey
        this.chainId = chainId
        this.accountAddress = accountAddress
    }

    fun isAddressValid(address: String): Boolean = transactionRepository.isAddressValid(address, chainId)

    private fun getERC20TokenDetails(address: String) =
        safeAccountRepository.getERC20TokenDetails(privateKey, chainId, address)
            .zipWith(tokenManager.getTokenIconURL(chainId, address),
                BiFunction<ERCToken, String, ERCToken> { token, logoURI ->
                    token.apply {
                        this.logoURI = if (logoURI != String.Empty) logoURI
                        else null
                    }
                }
            )

    private fun getERC721TokenDetails(address: String) =
        tokenManager.getERC721TokenDetails(privateKey, chainId, address)

    private fun getERC1155TokenDetails(address: String) =
        tokenManager.getERC1155TokenDetails(privateKey, chainId, address)

    private fun getTokenDetailsWithUnknownType(address: String) =
        getERC20TokenDetails(address)
            .onErrorResumeNext { getERC1155TokenDetails(address) }
            .onErrorResumeNext { getERC721TokenDetails(address) }
            .subscribeOn(Schedulers.io())


    fun getTokenDetails(address: String) =
        launchDisposable {
            getTokenDetailsWithUnknownType(address)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = {
                        token  = it
                    },
                    onError = {
                        it.message?.let { message ->
                            Timber.e("Checking Asset details error: $message")
                            logger.logToFirebase(message)
                        }
                        _errorLiveData.value = Event(ContractAddressIsNotSupportedThrowable())
                    }
                )
        }

    fun setTokenId(tokenId: String) {
        token?.tokenId = tokenId
    }

    fun addToken(token: ERCToken) =
        launchDisposable {
            tokenManager.saveToken(accountAddress, chainId, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _tokenAddedLiveData.value = Event(Unit) },
                    onError = {
                        Timber.e("Error during downloading token data: ${it.message}")
                        _errorLiveData.value = Event(it)
                    })
        }

    private fun updateMissingDetails(
        privateKey: String,
        chainId: Int,
        tokenAddress: String,
        tokenId: BigInteger,
        token: ERCToken
    ) = when (token.type) {
        TokenType.ERC721 -> tokenManager.updateMissingERC721TokensDetails(
            privateKey,
            chainId,
            tokenAddress,
            tokenId,
            String.Empty,
            token
        )
        TokenType.ERC1155 -> tokenManager.updateMissingERC1155TokensDetails(
            privateKey,
            chainId,
            tokenAddress,
            tokenId,
            String.Empty,
            token
        )
        else -> {
            Single.error(Throwable("Only ERC721 and ERC1155 can have missing details"))
        }
    }

    fun isOwner(tokenId: String) = token?.let {
        launchDisposable {
            Single.zip(
                tokenManager.isNftOwner(
                    it.type,
                    tokenId,
                    privateKey,
                    chainId,
                    it.address,
                    it.accountAddress
                ),
                updateMissingDetails(
                    privateKey,
                    chainId,
                    it.address,
                    tokenId.toBigInteger(),
                    it
                ),
                BiFunction<Boolean, ERCToken, Boolean> { isOwner, updatedToken ->
                    token?.mergeNftDetailsAfterTokenDiscovery(updatedToken)
                    isOwner
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _, _ -> _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onSuccess = { isOwner ->
                        _isOwnerLiveData.value = Event(Result.success(isOwner))
                    },
                    onError = {
                        Timber.e("Checking Asset details error: ${it.message}")
                        _isOwnerLiveData.value = Event(Result.failure(it))
                    }
                )
        }
    }


    fun getNetworkName() = NetworkManager.getNetwork(chainId).name
}