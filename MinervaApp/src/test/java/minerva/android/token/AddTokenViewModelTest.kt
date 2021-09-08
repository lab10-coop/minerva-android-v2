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
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AddTokenViewModelTest : BaseViewModelTest() {

    private val safeAccountRepository: SafeAccountRepository = mock()
    private val tokenManager: TokenManager = mock()

    private val viewModel = AddTokenViewModel(mock(), safeAccountRepository, tokenManager)

    private val tokenObserver: Observer<ERC20Token> = mock()
    private val tokenCaptor: KArgumentCaptor<ERC20Token> = argumentCaptor()

    private val addTokenObserver: Observer<Event<Unit>> = mock()
    private val addTokenCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val networks = listOf(
        Network(chainId = 1, httpRpc = "some_rpc")
    )

    @Test
    fun `Check getting Token details`() {
        NetworkManager.initialize(networks)

        whenever(safeAccountRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(
            Single.just(
                ERC20Token(
                    1,
                    name = "Some Token"
                )
            )
        )
        whenever(tokenManager.getTokenIconURL(any(), any())).thenReturn(Single.just("Cookie URL"))
        viewModel.run {
            addressDetailsLiveData.observeForever(tokenObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        tokenCaptor.run {
            verify(tokenObserver).onChanged(capture())
            firstValue.name shouldBeEqualTo "Some Token"
            firstValue.logoURI shouldBeEqualTo "Cookie URL"
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
            addToken(ERC20Token(1))
        }
        addTokenCaptor.run {
            verify(addTokenObserver).onChanged(capture())
        }
    }
}