package minerva.android.manage

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AddTokenViewModelTest : BaseViewModelTest() {

    private val smartContractRepository: SmartContractRepository = mock()
    private val tokenManager: TokenManager = mock()

    private val viewModel = AddTokenViewModel(mock(), smartContractRepository, tokenManager)

    private val tokenObserver: Observer<Token> = mock()
    private val tokenCaptor: KArgumentCaptor<Token> = argumentCaptor()

    private val addTokenObserver: Observer<Event<Unit>> = mock()
    private val addTokenCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `Check getting Token details` () {
        whenever(smartContractRepository.getERC20TokenDetails(any(), any(), any())).thenReturn(Single.just(Token(name = "Some Token")))
        viewModel.run {
            addressDetailsLiveData.observeForever(tokenObserver)
            getTokenDetails("0xS0m34ddr35")
        }
        tokenCaptor.run {
            verify(tokenObserver).onChanged(capture())
            firstValue.name shouldBeEqualTo  "Some Token"
        }
    }

    @Test
    fun `Check adding Token` () {
        whenever(tokenManager.saveToken(any(), any())).thenReturn(Completable.complete(), Completable.error(Throwable("Some error here!")))
        viewModel.run {
            tokenAddedLiveData.observeForever(addTokenObserver)
            addToken(Token())
        }
        addTokenCaptor.run {
            verify(addTokenObserver).onChanged(capture())
        }
    }

}