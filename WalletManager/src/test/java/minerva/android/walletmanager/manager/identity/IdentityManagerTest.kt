package minerva.android.walletmanager.manager.identity

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.RxTest
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.utils.DataProvider
import minerva.android.walletmanager.utils.DataProvider.walletConfig
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Test

class IdentityManagerTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val manager = IdentityManagerImpl(walletConfigManager, cryptographyRepository, localStorage)

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()) doReturn walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `Check that wallet manager returns correct value`() {
        manager.loadIdentity(0, "Identity").apply {
            index shouldBeEqualTo 0
            name shouldBeEqualTo "identityName1"
            privateKey shouldBeEqualTo "privateKey"
        }
        manager.loadIdentity(3, "Identity").apply {
            index shouldBeEqualTo walletConfig.newIndex
            name shouldBeEqualTo "Identity #8"
        }
        manager.loadIdentity(-1, "Identity").apply {
            index shouldBeEqualTo walletConfig.newIndex
            name shouldBeEqualTo "Identity #8"
        }
    }

    @Test
    fun `Check that wallet manager saves new identity`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val newIdentity = Identity(0, "identityName1")
        val test = manager.saveIdentity(newIdentity).test()
        val loadedIdentity = manager.loadIdentity(0, "Identity")
        test.assertNoErrors()
        loadedIdentity.name shouldBeEqualTo newIdentity.name
        loadedIdentity.privateKey shouldBeEqualTo "privateKey"
    }

    @Test
    fun `Check that wallet manager doesn't save when server error`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        val newIdentity = Identity(0, "identityName")
        manager.saveIdentity(newIdentity).test()
        val loadedIdentity = manager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldNotBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager removes correct identity`() {
        val identityToRemove = Identity(0, "identityName2", "", "privateKey", "address", DataProvider.data)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        manager.removeIdentity(identityToRemove).test()
        val loadedIdentity = manager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo identityToRemove.name
        loadedIdentity.isDeleted shouldBeEqualTo identityToRemove.isDeleted
    }

    @Test
    fun `Check that wallet manager doesn't remove identity when server error`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        manager.removeIdentity(identityToRemove).test()
        val loadedIdentity = manager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName2"
        loadedIdentity.isDeleted shouldBeEqualTo false
        loadedIdentity.personalData.size shouldBeEqualTo 3
    }

    @Test
    fun `Check that wallet manager doesn't remove identity, when there is only one active element`() {
        val identityToRemove = Identity(0)
        val walletConfig = WalletConfig(
            0, listOf(
                Identity(0, "identityName1", "", "privateKey", "address", DataProvider.data),
                Identity(1, "identityName1", "", "privateKey", "address", DataProvider.data, isDeleted = true)
            )
        )
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(DerivedKeys(0, "publicKey", "privateKey", "address"))
        )
        doNothing().whenever(walletConfigManager).initWalletConfig()
        manager.removeIdentity(identityToRemove).test()
        manager.loadIdentity(0, "Identity").apply {
            name shouldBeEqualTo "identityName1"
            isDeleted shouldBeEqualTo false
            personalData.size shouldBeEqualTo 3
        }
        walletConfig.identities.size shouldBeEqualTo 2
    }

    @Test
    fun `Check that wallet manager will not remove, when try to remove identity with wrong index`() {
        val identityToRemove = Identity(22)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        doNothing().whenever(walletConfigManager).initWalletConfig()
        manager.removeIdentity(identityToRemove).test()
        manager.loadIdentity(0, "Identity").apply {
            name shouldBeEqualTo "identityName1"
            isDeleted shouldBeEqualTo false
            personalData.size shouldBeEqualTo 3
        }
        walletConfig.identities.size shouldBeEqualTo 3
    }

    @Test
    fun `bind credential to identity success test`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever( walletConfigManager.findIdentityByDid(any())).thenReturn(Identity(1, address = "address", name = "identityName1"))
        manager.bindCredentialToIdentity(CredentialQrCode("iss", "type", loggedInDid = "did:ethr:address"))
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "identityName1"
            }
    }

    @Test
    fun `bind credential to identity error test`() {
        val error = NoBindedCredentialThrowable()
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        whenever( walletConfigManager.findIdentityByDid(any())).thenReturn(Identity(1, address = "address", name = "identityName1"))
        manager.bindCredentialToIdentity(CredentialQrCode(issuer = "iss", type = "type", loggedInDid = "did:ethr:address"))
            .test()
            .assertError {
                it is NoBindedCredentialThrowable
            }
    }

    @Test
    fun `removed binded credential to identity success test`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        manager.removeBindedCredentialFromIdentity(Credential("test", "type", loggedInIdentityDid = "did:ethr:address"))
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `removed binded credential to identity error test`() {
        val error = Throwable()
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))
        manager.removeBindedCredentialFromIdentity(Credential("test", "type", loggedInIdentityDid = "did:ethr:address"))
            .test()
            .assertError(error)
    }
}