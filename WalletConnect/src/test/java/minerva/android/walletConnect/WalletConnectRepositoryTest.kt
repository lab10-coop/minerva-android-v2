package minerva.android.walletConnect

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletConnect.repository.WalletConnectRepositoryImpl
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals

class WalletConnectRepositoryTest {

    private val client: WCClient = mock()
    private lateinit var clientMap: ConcurrentHashMap<String, WCClient>
    private lateinit var repository: WalletConnectRepository

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        clientMap = ConcurrentHashMap()
        clientMap["peerId"] = client
        repository = WalletConnectRepositoryImpl(client, clientMap)
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `is client map empty test`() {
        clientMap = ConcurrentHashMap()
        repository = WalletConnectRepositoryImpl(client, clientMap)
        val result = repository.isClientMapEmpty
        assertEquals(true, result)
    }

    @Test
    fun `is client map not empty test`() {
        val result = repository.isClientMapEmpty
        assertEquals(false, result)
    }

    @Test
    fun `get wallet connect clients test`() {
        val result = repository.walletConnectClients
        assertEquals(1, result.size)
    }

    @Test
    fun `get empty wallet connect clients test`() {
        val result = repository.walletConnectClients
        assertEquals(1, result.size)
    }

    @Test
    fun `reject session test`() {
        whenever(clientMap["peerId"]?.rejectSession(any())).thenReturn(true)
        repository.rejectSession("peerId")
        verify(clientMap["peerId"])!!.rejectSession(any())
    }

    @Test
    fun `kill session test`() {
        whenever(clientMap["peerId"]?.killSession()).thenReturn(true)
        repository.killSession("peerId")
        assertEquals(clientMap.size, 0)
    }
}