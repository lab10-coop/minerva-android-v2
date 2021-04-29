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

    private val viewModel = AuthenticationViewModel(localStorage)

    private val protectKeysObserver: Observer<Event<Boolean>> = mock()
    private val protectKeysCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    private val protectTransactionsObserver: Observer<Event<Boolean>> = mock()
    private val protectTransactionsCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    @Before
    fun setupLiveDataObservers() {
        viewModel.protectKeysLiveData.observeForever(protectKeysObserver)
        viewModel.protectTransactionsLiveData.observeForever(protectTransactionsObserver)
    }

    @Test
    fun `Check that initializing view model works fine`() {
        whenever(localStorage.isProtectKeysEnabled).thenReturn(true)
        whenever(localStorage.isProtectTransactionsEnabled).thenReturn(false)
        viewModel.init()
        protectKeysCaptor.run { verify(protectKeysObserver).onChanged(capture()) }
        protectTransactionsCaptor.run { verify(protectTransactionsObserver).onChanged(capture()) }
    }

    @Test
    fun `Check that toggling protect keys works fine`() {
        whenever(localStorage.isProtectKeysEnabled).thenReturn(true)
        whenever(localStorage.isProtectTransactionsEnabled).thenReturn(true)
        viewModel.toggleProtectKeys()
        protectKeysCaptor.run {
            verify(protectKeysObserver).onChanged(capture())
            firstValue.peekContent() shouldBeEqualTo false
        }
        protectTransactionsCaptor.run { verify(protectTransactionsObserver).onChanged(capture()) }
    }

    @Test
    fun `Check that toggling protect transactions works fine`() {
        whenever(localStorage.isProtectTransactionsEnabled).thenReturn(false)
        viewModel.protectTransactionsLiveData.observeForever(protectTransactionsObserver)
        viewModel.toggleProtectTransactions()
        protectTransactionsCaptor.run { verify(protectTransactionsObserver).onChanged(capture()) }
    }
}