package minerva.android

import android.os.HandlerThread
import android.os.Looper
import com.nhaarman.mockitokotlin2.mock
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.createWalletConfigProviderModule
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.Empty
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.*
import javax.net.ssl.HttpsURLConnection

class MinervaApiTest : KoinTest {

    private lateinit var mockServer: MockWebServer
    private val api: MinervaApi by inject()

    private val map1: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "James Adams",
        "Email" to "ja@email.com",
        "Date of Brith" to "13.03.1974"
    )
    private val map2: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991"
    )

    @Before
    fun setUp() {
        configureMockServer()
        configureDi()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
    }

    private fun configureDi() {
        mockkConstructor(HandlerThread::class)
        val looper: Looper = mock()
        every { anyConstructed<HandlerThread>().looper } returns looper
        every { anyConstructed<HandlerThread>().run() } returns Unit
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns looper
        koinApplication {
            startKoin {
                modules(createWalletConfigProviderModule(true, mockServer.url("/").toString()))
            }
        }
    }

    @After
    fun tearDown() {
        stopMockServer()
        stopKoin()
        RxJavaPlugins.reset()
    }

    @Test
    fun `get wallet config should return success`() {
        mockHttpResponse(
            mockServer = mockServer,
            responseCode = HttpsURLConnection.HTTP_OK,
            json = "{\"state\": \"success\", \"message\": \"The file has been loaded!\", \"data\": {\"version\": \"1\",\"identities\" : [{\"name\": \"test1\",\"index\": \"1\",\"data\": {\"name\": \"James\",\"email\": \"james@test.pl\" } }],\"values\" : [] } }"
        )
        api.getWalletConfig(publicKey = "12345678").test().assertValue {
            it.state == "success"
        }
    }

    @Test
    fun `get wallet config should return empty json`() {
        mockHttpResponse(
            mockServer = mockServer,
            responseCode = HttpsURLConnection.HTTP_OK,
            json = "{}"
        )
        api.getWalletConfig(publicKey = "12345678").test().assertValue {
            it.state == String.Empty
        }
    }

    @Test
    fun `send wallet config should return success`() {
        mockHttpResponse(
            mockServer = mockServer,
            responseCode = HttpsURLConnection.HTTP_OK,
            json = "{}"
        )

        api.saveWalletConfig(
            publicKey = "12345678", walletConfigPayload = WalletConfigPayload(
                _version = 1, _identityPayloads = listOf(
                    IdentityPayload(_index = 0, _name = "test0", _data = map1),
                    IdentityPayload(_index = 1, _name = "test1", _data = map2)
                )
            )
        ).test().await().assertComplete()
    }

    @Test(expected = Throwable::class)
    fun `send wallet config should return error`() {
        mockHttpResponse(
            mockServer = mockServer,
            responseCode = HttpsURLConnection.HTTP_BAD_REQUEST,
            json = "{}"
        )

        api.saveWalletConfig(
            publicKey = "12345678", walletConfigPayload = WalletConfigPayload(
                _version = 1, _identityPayloads = listOf(
                    IdentityPayload(_index = 0, _name = "test0", _data = map2),
                    IdentityPayload(_index = 1, _name = "test1", _data = map2)
                )
            )
        ).test().await().assertError(Throwable())
    }

    private fun mockHttpResponse(mockServer: MockWebServer, responseCode: Int, json: String) =
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(responseCode)
                .setBody(json)
        )

    private fun configureMockServer() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    private fun stopMockServer() {
        mockServer.shutdown()
    }
}