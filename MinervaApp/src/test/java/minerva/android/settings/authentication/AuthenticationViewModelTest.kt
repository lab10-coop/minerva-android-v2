package minerva.android.settings.authentication

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.should
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class AuthenticationViewModelTest : BaseViewModelTest() {

    private val localStorage: LocalStorage = mock()

    private lateinit var viewModel: AuthenticationViewModel

    private val protectKeysObserver: Observer<Event<Boolean>> = mock()
    private val protectKeysCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    private val protectTransactionsObserver: Observer<Event<Boolean>> = mock()
    private val protectTransactionsCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    @Before
    fun setupLiveDataObservers() {
        localStorage.run {
            whenever(isProtectKeysEnabled).thenReturn(true)
            whenever(isProtectTransactionsEnabled).thenReturn(false)
            viewModel = AuthenticationViewModel(this).apply {
                protectKeysLiveData.observeForever(protectKeysObserver)
                protectTransactionsLiveData.observeForever(protectTransactionsObserver)
            }
        }
    }

    @Test
    fun `Check that initializing view model works fine`() {
        protectKeysCaptor.run { verify(protectKeysObserver).onChanged(capture()) }
        protectTransactionsCaptor.run { verify(protectTransactionsObserver).onChanged(capture()) }
    }

    @Test
    fun `Check that toggling protect keys works fine`() {
        viewModel.toggleProtectKeys()
        protectKeysCaptor.run {
            verify(protectKeysObserver, times(2)).onChanged(capture())
            firstValue.peekContent() shouldBeEqualTo true
            secondValue.peekContent() shouldBeEqualTo false
        }
        protectTransactionsCaptor.run { verify(protectTransactionsObserver, times(2)).onChanged(capture()) }
    }

    @Test
    fun `Check that toggling protect transactions works fine`() {
        viewModel.toggleProtectTransactions()
        protectTransactionsCaptor.run { verify(protectTransactionsObserver, times(2)).onChanged(capture()) }
    }
}