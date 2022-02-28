package minerva.android.token

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AddTokenViewModelTest : BaseViewModelTest() {

    private val safeAccountRepository: SafeAccountRepository = mock()
    private val tokenManager: TokenManager = mock()

    private val viewModel = AddTokenViewModel(mock(), safeAccountRepository, tokenManager, mock())

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val tokenObserver: Observer<ERCToken> = mock()
    private val tokenCaptor: KArgumentCaptor<ERCToken> = argumentCaptor()

    private val addTokenObserver: Observer<Event<Unit>> = mock()
    private val addTokenCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val networks = listOf(
        Network(chainId = 1, httpRpc = "some_rpc")
    )

    @Test
    fun `Check getting ERC20 Token details`() {
        NetworkManager.initialize(networks)

        whenever(safeAccountRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(
            Single.just(
                ERCToken(1, name = "Some Token", type = TokenType.ERC20)
            )
        )
        whenever(tokenManager.getERC721TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC1155TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getTokenIconURL(any(), any())).thenReturn(Single.just("Cookie URL"))
        viewModel.run {
            tokenLiveData.observeForever(tokenObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        tokenCaptor.run {
            verify(tokenObserver).onChanged(capture())
            firstValue.name shouldBeEqualTo "Some Token"
            firstValue.logoURI shouldBeEqualTo "Cookie URL"
            firstValue.type shouldBeEqualTo TokenType.ERC20
        }
    }

    @Test
    fun `Check getting ERC721 Token details`() {
        NetworkManager.initialize(networks)

        whenever(safeAccountRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC721TokenDetails(any(), any(), any())).thenReturn(
            Single.just(
                ERCToken(1, name = "Some Token", type = TokenType.ERC721)
            )
        )
        whenever(tokenManager.getERC1155TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getTokenIconURL(any(), any())).thenReturn(Single.just("Cookie URL"))
        viewModel.run {
            tokenLiveData.observeForever(tokenObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        tokenCaptor.run {
            verify(tokenObserver).onChanged(capture())
            firstValue.name shouldBeEqualTo "Some Token"
            firstValue.type shouldBeEqualTo TokenType.ERC721
        }
    }

    @Test
    fun `Check getting ERC1155 Token details`() {
        NetworkManager.initialize(networks)

        whenever(safeAccountRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC721TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC1155TokenDetails(any(), any(), any())).thenReturn(
            Single.just(
                ERCToken(1, name = "Some Token", type = TokenType.ERC1155)
            )
        )
        whenever(tokenManager.getTokenIconURL(any(), any())).thenReturn(Single.just("Cookie URL"))
        viewModel.run {
            tokenLiveData.observeForever(tokenObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        tokenCaptor.run {
            verify(tokenObserver).onChanged(capture())
            firstValue.name shouldBeEqualTo "Some Token"
            firstValue.type shouldBeEqualTo TokenType.ERC1155
        }
    }

    @Test
    fun `Check unsupported address`() {
        NetworkManager.initialize(networks)

        whenever(safeAccountRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC721TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getERC1155TokenDetails(any(), any(), any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        whenever(tokenManager.getTokenIconURL(any(), any())).thenReturn(Single.just("Cookie URL"))

        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            times(1)
        }
    }

    @Test
    fun `Check adding Token`() {
        whenever(tokenManager.saveToken(any(), any(), any())).thenReturn(
            Completable.complete(),
            Completable.error(Throwable("Some error here!"))
        )
        viewModel.run {
            tokenAddedLiveData.observeForever(addTokenObserver)
            addToken(ERCToken(1, type = TokenType.ERC20))
        }
        addTokenCaptor.run {
            verify(addTokenObserver).onChanged(capture())
        }
    }
}