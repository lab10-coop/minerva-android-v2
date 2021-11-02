package minerva.android.accounts.nft.viewmodel

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import minerva.android.BaseViewModelTest
import minerva.android.accounts.nft.model.NftItem
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import org.junit.Test
import java.math.BigDecimal

class NftCollectionViewModelTest : BaseViewModelTest() {

    private val accountManager: AccountManager = mock()
    private val tokenManager: TokenManager = mock()
    private val accountId = 1
    private val collectionAddress = "collectionAddress"
    private val viewModel: NftCollectionViewModel =
        NftCollectionViewModel(accountManager, tokenManager, accountId, collectionAddress)

    private val nftListObserver: Observer<List<NftItem>> = mock()
    private val nftListCaptor: KArgumentCaptor<List<NftItem>> = argumentCaptor()

    private val loadingObserver: Observer<Boolean> = mock()
    private val loadingCaptor: KArgumentCaptor<Boolean> = argumentCaptor()

    private val token1 = ERCToken(
        100,
        "Token1",
        "T1",
        collectionAddress,
        String.Empty,
        accountAddress = "accountAddress",
        type = TokenType.ERC721,
        description = "T1 desctiption",
        contentUri = "contentUriT1",
        tokenId = "1"
    )
    private val token2 = ERCToken(
        100,
        "Token2",
        "T2",
        collectionAddress,
        String.Empty,
        accountAddress = "accountAddress",
        type = TokenType.ERC721,
        description = "T2 desctiption",
        contentUri = "contentUriT2",
        tokenId = "2"
    )
    private val token3 = ERCToken(
        100,
        "Token3",
        "T3",
        collectionAddress,
        String.Empty,
        accountAddress = "accountAddress",
        type = TokenType.ERC721,
        description = "T3 desctiption",
        contentUri = "contentUriT3",
        tokenId = "3"
    )
    private val account = Account(
        accountId,
        privateKey = "privateKey",
        chainId = 100,
        address = "accountAddress",
        accountTokens = mutableListOf(
            AccountToken(token1, BigDecimal.ONE),
            AccountToken(token2, BigDecimal.ZERO),
            AccountToken(token3, BigDecimal.ONE)
        )
    )

    @Test
    fun `get nft list success`() {
        whenever(accountManager.loadAccount(accountId)).thenReturn(account)
        whenever(
            tokenManager.getNftsPerAccount(
                account.chainId,
                account.address,
                collectionAddress
            )
        ).thenReturn(
            listOf(token1, token2, token3)
        )
        viewModel.nftListLiveData.observeForever(nftListObserver)
        viewModel.loadingLiveData.observeForever(loadingObserver)
        viewModel.getNftForCollection()
        loadingCaptor.run {
            verify(loadingObserver, times(2)).onChanged(capture())
            firstValue && !secondValue
        }
        nftListCaptor.run {
            verify(nftListObserver, times(2)).onChanged(capture())
            firstValue == emptyList<NftItem>() &&
                    secondValue[0].name == token1.name &&
                    firstValue[0].tokenId == token1.tokenId &&
                    firstValue[0].contentUrl == token1.contentUri &&
                    firstValue[0].description == token1.description &&
                    firstValue[0].tokenAddress == token1.address &&
                    firstValue[1].name == token3.name &&
                    firstValue[1].tokenId == token3.tokenId &&
                    firstValue[1].contentUrl == token3.contentUri &&
                    firstValue[1].description == token3.description &&
                    firstValue[1].tokenAddress == token3.address
        }
    }
}

