package minerva.android.accounts.nft.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.accounts.nft.model.NftItem
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import java.math.BigDecimal

class NftCollectionViewModel(
    private val accountManager: AccountManager,
    private val tokenManager: TokenManager,
    private val accountId: Int,
    private val collectionAddress: String
) : BaseViewModel() {

    private val nftList = mutableListOf<NftItem>()

    private val _nftListLiveData = MutableLiveData<List<NftItem>>(nftList)
    val nftListLiveData: LiveData<List<NftItem>> get() = _nftListLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean> get() = _loadingLiveData

    fun getNftForCollection() {
        _loadingLiveData.value = true
        accountManager.loadAccount(accountId).let { account ->
            val visibleTokens = account.getVisibleTokens()
            tokenManager.getNftsPerAccount(account.chainId, account.address, collectionAddress).forEach { token ->
                with(token) {
                    if (visibleTokens.find { accountToken -> tokenId == accountToken.token.tokenId } != null)
                        nftList.add(NftItem(address, tokenId ?: String.Empty, description, contentUri, name))
                }
            }
            updateList()
        }
    }

    private fun Account.getVisibleTokens() = accountTokens.filter { accountToken ->
        accountToken.token.address.equals(
            collectionAddress,
            true
        ) && accountToken.token.type.isERC721() && accountToken.currentRawBalance > BigDecimal.ZERO
    }

    private fun updateList() {
        _loadingLiveData.value = false
        _nftListLiveData.value = nftList.sortedBy { it.tokenId }
    }
}