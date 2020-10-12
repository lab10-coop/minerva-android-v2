package minerva.android.walletmanager.manager.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.Profile
import minerva.android.walletmanager.manager.RxTest
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.CredentialType
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ServiceManagerTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val servicesApi: ServicesApi = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val repository = ServiceManagerImpl(walletConfigManager, servicesApi, cryptographyRepository)

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
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
                    Pair("name", "test"),
                    Pair("callback", "test"),
                    Pair("iss", "test"),
                    Pair("requested", "test")
                )
            )
        )
        repository.decodeJwtToken("token")
            .test()
            .assertComplete()
            .assertValue {
                (it as ServiceQrCode).run {
                    it.callback == "test" && it.issuer == "test"
                }
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
    fun `decode payment token error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.error(error)
        repository.run {
            decodeThirdPartyRequestToken("jwtToken")
                .test()
                .assertError(error)
        }
    }

    @Test
    fun `decode qr code response success test`() {
        whenever(cryptographyRepository.decodeJwtToken(any())).thenReturn(
            Single.just(
                hashMapOf(
                    Pair("name", "test"),
                    Pair("callback", "test"),
                    Pair("iss", "test"),
                    Pair("requested", "test")
                )
            )
        )
        repository.decodeJwtToken("token")
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.issuer == "test"
            }
    }

    @Test
    fun `decode qr code response error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.error(error)
        repository.decodeJwtToken("token")
            .test()
            .assertError(error)
    }

    @Test
    fun `get logged in identity test`() {
        val expected = Identity(0, name = "tom", publicKey = "publicKey")
        whenever(walletConfigManager.getLoggedInIdentityByPublicKey(any())) doReturn expected
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

    @Test
    fun `remove service success test`() {
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.complete()
        repository.removeService("1")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `remove service error test`() {
        val error = Throwable()
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.error(error)
        repository.removeService("1")
            .test()
            .assertError(error)
    }

    @Test
    fun `update credential success test`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever( walletConfigManager.findIdentityByDid(any())).thenReturn(Identity(1, address = "address", name = "identityName1"))
        repository.updateBindedCredential(CredentialQrCode(issuer = "iss", loggedInDid = "did:ethr:address", type = CredentialType.VERIFIABLE_CREDENTIAL, membershipType = CredentialType.AUTOMOTIVE_CLUB))
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "identityName1"
            }
    }

    @Test
    fun `update credential success error`() {
        val error = Throwable()
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever( walletConfigManager.findIdentityByDid(any())).thenReturn(Identity(1, address = "address", name = "identityName1"))
        repository.updateBindedCredential(CredentialQrCode(issuer = "iss", loggedInDid = "did:ethr:address", type = CredentialType.VERIFIABLE_CREDENTIAL, membershipType = CredentialType.AUTOMOTIVE_CLUB))
            .test()
            .assertError(error)
    }
}