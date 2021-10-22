package minerva.android.accounts.nft.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.nft.model.NftItem
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.token.NftVisibilityResult

class NftCollectionViewModel(
    private val accountManager: AccountManager,
    private val tokenManager: TokenManager,
    private val accountId: Int,
    private val collectionAddress: String
) : BaseViewModel() {

    private val nftList = mutableListOf<NftItem>()

    private val _nftListLiveData = MutableLiveData<List<NftItem>>(nftList)
    val nftListLiveData: LiveData<List<NftItem>> get() = _nftListLiveData

    private val _errorLiveData = MutableLiveData<Unit>()
    val errorLiveData: LiveData<Unit> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean> get() = _loadingLiveData

    fun getNftForCollection() {
        launchDisposable {
            _loadingLiveData.value = true
            accountManager.loadAccount(accountId).let { account ->
                tokenManager.getNftsPerAccountTokenFlowable(account.privateKey, account.chainId, account.address, collectionAddress)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onComplete = { updateList() },
                        onNext = { result -> result.handleResult() },
                        onError = {
                            updateList()
                            _errorLiveData.value = Unit
                        }
                    )
            }
        }
    }

    private fun updateList() {
        _loadingLiveData.value = false
        _nftListLiveData.value = nftList.sortedBy { it.tokenId }
    }

    private fun NftVisibilityResult.handleResult() {
        if (isVisible) {
            with(token) {
                nftList.add(
                    NftItem(address, tokenId!!, description, contentUri, name)
                )
            }
        }
    }
}