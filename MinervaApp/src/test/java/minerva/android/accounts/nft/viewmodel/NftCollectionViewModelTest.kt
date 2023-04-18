package minerva.android.accounts.nft.viewmodel

import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.nft.model.NftItem
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.NftContent
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class NftCollectionViewModelTest : BaseViewModelTest() {

    private val accountManager: AccountManager = mock()
    private val tokenManager: TokenManager = mock()

    private val transactionRepository: TransactionRepository = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val accountId = 1
    private val collectionAddressToJson = Gson().toJson(listOf("collectionAddress"))
    private val collectionAddress = "collectionAddress"
    private val isGroup = false
    private val viewModel: NftCollectionViewModel =
        NftCollectionViewModel(accountManager, tokenManager, transactionRepository, walletActionsRepository, accountId, collectionAddressToJson, isGroup).apply {
            transaction = Transaction()
        }

    private val transactionCompletedObserver: Observer<Event<Any>> = mock()
    private val transactionCompletedCaptor: KArgumentCaptor<Event<Any>> = argumentCaptor()

    private val saveActionFailedCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()


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
        nftContent = NftContent("contentUriT1", description =  "T1 desctiption"),
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
        nftContent = NftContent("contentUriT2", description =  "T2 desctiption"),
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
        nftContent = NftContent("contentUriT3", description =  "T3 desctiption"),
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
                    firstValue[0].nftContent.imageUri == token1.nftContent.imageUri &&
                    firstValue[0].nftContent.contentType == token1.nftContent.contentType &&
                    firstValue[0].nftContent.animationUri == token1.nftContent.animationUri &&
                    firstValue[0].nftContent.background == token1.nftContent.background &&
                    firstValue[0].nftContent.description == token1.nftContent.description &&
                    firstValue[0].tokenAddress == token1.address &&
                    firstValue[1].name == token3.name &&
                    firstValue[1].tokenId == token3.tokenId &&
                    firstValue[1].nftContent.imageUri == token3.nftContent.imageUri &&
                    firstValue[1].nftContent.contentType == token3.nftContent.contentType &&
                    firstValue[1].nftContent.animationUri == token3.nftContent.animationUri &&
                    firstValue[1].nftContent.background == token3.nftContent.background &&
                    firstValue[1].nftContent.description == token3.nftContent.description &&
                    firstValue[1].tokenAddress == token3.address
        }
    }

    @Test
    fun `send erc721 transaction test success and wallet action succeed`() {
        whenever(transactionRepository.transferERC721Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.selectedItem = NftItem(isERC1155 = false)
        NetworkManager.initialize(listOf(Network(chainId = 1)))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send erc721 transaction test success and balance changed succeed`() {
        whenever(transactionRepository.transferERC721Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.selectedItem = NftItem(isERC1155 = false, balance = BigDecimal(13))
        NetworkManager.initialize(listOf(Network(chainId = 1)))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
            viewModel.selectedItem.balance shouldBeEqualTo BigDecimal.ONE
        }
    }

    @Test
    fun `send erc1155 transaction test success and wallet action succeed`() {
        whenever(transactionRepository.transferERC1155Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.selectedItem = NftItem(isERC1155 = true)
        NetworkManager.initialize(listOf(Network(chainId = 1)))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send erc1155 transaction test success and balance changed`() {
        whenever(transactionRepository.transferERC1155Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.selectedItem = NftItem(isERC1155 = true, balance = BigDecimal(13))
        NetworkManager.initialize(listOf(Network(chainId = 1)))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
            viewModel.selectedItem.balance shouldBeEqualTo BigDecimal.ONE
        }
    }

    @Test
    fun `send erc1155 transaction test success and wallet action failed`() {
        val error = Throwable()
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.transferERC1155Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        NetworkManager.initialize(listOf(Network(chainId = 1)))

        viewModel.selectedItem = NftItem(isERC1155 = true)
        viewModel.run {
            saveWalletActionFailedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        saveActionFailedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send erc1155 transaction test error and send wallet action succeed`() {
        whenever(transactionRepository.transferERC1155Token(any(), any())).thenReturn(
            Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.selectedItem = NftItem(isERC1155 = true)
        NetworkManager.initialize(listOf(Network(chainId = 1)))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            account
            sendTransaction("123", BigDecimal(12))
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `calculate transaction cost test`() {
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal(2))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("$")
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0, coinRate = 2.0))
        viewModel.setGasLimit(BigInteger.ONE)
        viewModel.setGasPrice(BigDecimal(2))
        viewModel.transactionCost.cost shouldBeEqualTo BigDecimal(2)
        viewModel.transactionCost.fiatCost shouldBeEqualTo "$ 4.00"
    }

    @Test
    fun `get available funds test`() {
        viewModel.selectedItem = NftItem(balance = BigDecimal(6))
        val result = viewModel.getAllAvailableFunds()
        result shouldBeEqualTo BigDecimal(6)
    }

    @Test
    fun `is address valid success`() {
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0))
        whenever(transactionRepository.isAddressValid(any(), anyOrNull())).thenReturn(true)
        val result = viewModel.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(accountManager.loadAccount(any())).thenReturn(Account(0))
        whenever(transactionRepository.isAddressValid(any(), anyOrNull())).thenReturn(false)
        val result = viewModel.isAddressValid("eeee")
        assertEquals(false, result)
    }

}

