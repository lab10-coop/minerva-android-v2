package minerva.android.walletmanager.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.Profile
import minerva.android.walletmanager.manager.services.ServiceManagerImpl
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ServiceManagerTest {

    private val walletConfigManager: WalletConfigManager = mock()
    private val servicesApi: ServicesApi = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val repository = ServiceManagerImpl(walletConfigManager, servicesApi, cryptographyRepository)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `painless login with identity test success`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.saveService(any())).thenReturn(Completable.complete())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.painlessLogin("url", "jwtToken", Identity(1), Service("1"))
            .test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test success`() {
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        repository.painlessLogin("url", "jwtToken", IncognitoIdentity(), Service("1"))
            .test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test error`() {
        val error = Throwable()
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.error(error))
        repository.painlessLogin("url", "jwtToken", IncognitoIdentity(), Service("1"))
            .test()
            .assertError(error)
    }

    @Test
    fun `decode jwt token success test`() {
        whenever(cryptographyRepository.decodeJwtToken(any())).thenReturn(
            Single.just(
                hashMapOf(
                    Pair("callback", "test"),
                    Pair("iss", "test"),
                    Pair("requested", "test")
                )
            )
        )
        repository.decodeQrCodeResponse("token")
            .test()
            .assertComplete()
            .assertValue {
                it.callback == "test" && it.issuer == "test"
            }
    }

    @Test
    fun `save service test`() {
        whenever(walletConfigManager.saveService(any())).thenReturn(Completable.complete())
        repository.saveService(Service())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save service test error`() {
        val error = Throwable()
        whenever(walletConfigManager.saveService(any())).thenReturn(Completable.error(error))
        repository.saveService(Service()).test().assertError(error)
    }

    @Test
    fun `decode payment token success test`() {
        val jwtData = mapOf<String, Any?>(
            PaymentRequest.AMOUNT to "amount", PaymentRequest.IBAN to "iban",
            PaymentRequest.RECIPIENT to "recipient", PaymentRequest.SERVICE_NAME to "name", PaymentRequest.SERVICE_SHORT_NAME to "short",
            PaymentRequest.URL to "url"
        )
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.just(jwtData)
        repository.decodePaymentRequestToken("jwtToken")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `decode payment token error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.error(error)
        repository.run {
            decodePaymentRequestToken("jwtToken")
                .test()
                .assertError(error)
        }
    }

    @Test
    fun `decode qr code response success test`() {
        val jwtData = mapOf<String, Any?>(PaymentRequest.URL to "url", "iss" to "123", "requested" to arrayListOf<String>("test"))
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.just(jwtData)
        repository.decodeQrCodeResponse("token")
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.issuer == "123"
            }
    }

    @Test
    fun `decode qr code response error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.error(error)
        repository.decodeQrCodeResponse("token")
            .test()
            .assertError(error)
    }

    @Test
    fun `get logged in identity test`() {
        val expected = Identity(0, name = "tom", publicKey = "publicKey")
        whenever(walletConfigManager.getLoggedInIdentity(any())) doReturn expected
        repository.run {
            val result = getLoggedInIdentity("publicKey")
            assertEquals(result, expected)
        }
    }

    @Test
    fun `get logged in identity error test`() {
        val expected = Identity(0, name = "tom")
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(identities = listOf(expected))
        repository.run {
            val result = getLoggedInIdentity("publicKey")
            assertEquals(result, null)
        }
    }

    @Test
    fun `get logged in identity public key test`() {
        whenever(walletConfigManager.getLoggedInIdentityPublicKey(any())) doReturn "iss"
        repository.run {
            val result = getLoggedInIdentityPublicKey("iss")
            assertEquals(result, "iss")
        }
    }

    @Test
    fun `get logged in identity public key error test`() {
        whenever(walletConfigManager.getLoggedInIdentityPublicKey(any())) doReturn ""
        repository.run {
            val result = getLoggedInIdentityPublicKey("iss")
            assertEquals(result, "")
        }
    }

    @Test
    fun `is already logged in test`() {
        whenever(walletConfigManager.isAlreadyLoggedIn(any())) doReturn true
        repository.run {
            val result = isAlreadyLoggedIn(ServiceType.CHARGING_STATION)
            assertEquals(result, true)
        }
    }

    @Test
    fun `is already logged in error test`() {
        whenever(walletConfigManager.isAlreadyLoggedIn(any())) doReturn false
        repository.run {
            val result = isAlreadyLoggedIn("issuer")
            assertEquals(result, false)
        }
    }

    @Test
    fun `create jwtToken success test`() {
        whenever(cryptographyRepository.createJwtToken(any(), any())) doReturn Single.just("token")
        repository.createJwtToken(mapOf("name" to "tom"))
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "token"
            }
    }

    @Test
    fun `create jwtToken error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.createJwtToken(any(), any())) doReturn Single.error(error)
        repository.createJwtToken(mapOf("name" to "tom"))
            .test()
            .assertError(error)
    }
}