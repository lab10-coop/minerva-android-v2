package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.configProvider.localProvider.LocalWalletConfigProvider
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.configProvider.repository.HttpNotFoundException
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.WalletConfigNotFoundThrowable
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManagerImpl
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MockDataProvider
import minerva.android.walletmanager.utils.MockDataProvider.localWalletConfigPayload
import minerva.android.walletmanager.utils.MockDataProvider.onlineWalletConfigResponse
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class WalletConfigManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val localWalletConfigProvider: LocalWalletConfigProvider = mock()
    private val localStorage: LocalStorage = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val minervaApi: MinervaApiRepository = mock()

    private val walletManager =
        WalletConfigManagerImpl(keyStoreRepository, cryptographyRepository, localWalletConfigProvider, localStorage, minervaApi)

    private val walletConfigObserver: Observer<Event<WalletConfig>> = mock()
    private val walletConfigCaptor: KArgumentCaptor<Event<WalletConfig>> = argumentCaptor()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    private fun mockWallet() {
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.just(WalletConfigPayload()))
        whenever(minervaApi.getWalletConfigVersion(any())).doReturn(Single.just(1))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(
            MasterSeed(
                _seed = "seed",
                _privateKey = "privateKey",
                _publicKey = "public"
            )
        )
        whenever(localWalletConfigProvider.saveWalletConfig(any())).thenReturn(Single.just(WalletConfigPayload()))
        whenever(localWalletConfigProvider.getWalletConfig()).thenReturn(Single.just(localWalletConfigPayload))
        whenever(minervaApi.getWalletConfig(any())).thenReturn(Single.just(onlineWalletConfigResponse))
        whenever(minervaApi.saveWalletActions(any(), any())).doReturn(Completable.complete())
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), eq(0), any(), any()))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), eq(1), any(), any()))
            .thenReturn(Single.just(DerivedKeys(1, "publicKey", "privateKey", "address")))
        whenever(cryptographyRepository.calculateDerivedKeysSingle(any(), any(), eq(2), any(), any()))
            .thenReturn(Single.just(DerivedKeys(2, "publicKey", "privateKey", "address")))
        whenever(localStorage.getProfileImage(any())).thenReturn(String.Empty)
        whenever(localStorage.isBackupAllowed).thenReturn(true)
        NetworkManager.initialize(MockDataProvider.networks)
    }

    @Test
    fun `fetch wallet config when backup is not allowed`() {
        mockWallet()
        whenever(localStorage.isBackupAllowed).thenReturn(false)
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
            firstValue.peekContent().identities[0].name shouldBeEqualTo "IdentityName1"
        }
    }

    @Test
    fun `fetch wallet config when api error occurs`() {
        val error = Throwable()
        mockWallet()
        whenever(minervaApi.getWalletConfig(any())).thenReturn(Single.error(error))

        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
            firstValue.peekContent().identities[0].name shouldBeEqualTo "IdentityName1"
        }
    }

    @Test
    fun `Check that loading wallet config returns success`() {
        mockWallet()
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
            firstValue.peekContent().identities[0].name shouldBeEqualTo "IdentityName1"
        }
    }

    @Test
    fun `Create default walletConfig should return success`() {
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.just(WalletConfigPayload()))
        NetworkManager.initialize(listOf(Network(chainId = 0)))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertSubscribed().awaitCount(1).assertComplete()
    }

    @Test
    fun `Create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.error(throwable))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertSubscribed().awaitCount(1).assertError(throwable)
    }

    @Test
    fun `restore wallet config success test`() {
        whenever(minervaApi.getWalletConfig(any())).thenReturn(Single.just(WalletConfigPayload(_version = 1)))
        doNothing().whenever(keyStoreRepository).encryptMasterSeed(any())
        whenever(localWalletConfigProvider.saveWalletConfig(any())).thenReturn(Single.just(WalletConfigPayload()))
        walletManager.restoreWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertComplete()
    }

    @Test
    fun `restore wallet config error test`() {
        val error = Throwable()
        doNothing().whenever(keyStoreRepository).encryptMasterSeed(any())
        whenever(localWalletConfigProvider.saveWalletConfig(any())).thenReturn(Single.just(WalletConfigPayload()))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(minervaApi.getWalletConfig(any())).thenReturn(Single.error(error))
        walletManager.restoreWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertError(error)
    }

    @Test
    fun `restore wallet config http not found error test`() {
        val error = HttpNotFoundException()
        doNothing().whenever(keyStoreRepository).encryptMasterSeed(any())
        whenever(localWalletConfigProvider.saveWalletConfig(any())).thenReturn(Single.just(WalletConfigPayload()))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(minervaApi.getWalletConfig(any())).thenReturn(Single.error(error))
        walletManager.restoreWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertError {
                it is WalletConfigNotFoundThrowable
            }
    }

    @Test
    fun `save service test success`() {
        mockWallet()
        walletManager.initWalletConfig()
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.just(WalletConfigPayload()))
        walletManager.saveService(Service())
            .test()
            .assertSubscribed()
            .awaitCount(1)
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save service test error`() {
        mockWallet()
        val error = Throwable()
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.error(error))
        walletManager.initWalletConfig()
        walletManager.saveService(Service()).test().assertSubscribed().awaitCount(1).assertComplete()
    }

    @Test
    fun `get value test success`() {
        mockWallet()
        walletManager.apply {
            walletManager.initWalletConfig()
            val result = getAccount(1)
            assertEquals(result?.address, "contractAddress")
        }
    }

    @Test
    fun `get safe account master owner key test`() {
        mockWallet()
        walletManager.run {
            initWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("contractAddress")
            assertEquals(result, "privateKey")
        }
    }

    @Test
    fun `get safe account master owner key error test`() {
        mockWallet()
        walletManager.run {
            initWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "")
        }
    }

    @Test
    fun `update safe account owners test`() {
        mockWallet()
        walletManager.apply {
            initWalletConfig()
            updateSafeAccountOwners(0, listOf("owner"))
                .test()
                .assertNoErrors()
                .assertSubscribed()
                .awaitCount(1)
                .assertComplete()
                .assertValue {
                    it[0] == "owner"
                }
        }
    }

    @Test
    fun `update safe account owners error test`() {
        val error = Throwable()
        mockWallet()
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.error(error))
        walletManager.apply {
            initWalletConfig()
            updateSafeAccountOwners(0, listOf("owner"))
                .test()
                .assertSubscribed()
                .awaitCount(1)
                .assertComplete()
        }
    }

    @Test
    fun `update wallet config success`() {
        mockWallet()
        walletManager.initWalletConfig()
        walletManager.updateWalletConfig(WalletConfig())
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
        }
    }

    @Test
    fun `update wallet config error when backup is not allowed`() {
        mockWallet()
        whenever(localStorage.isBackupAllowed).thenReturn(false)
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletManager.updateWalletConfig(WalletConfig())
            .test()
            .assertError {
                it is AutomaticBackupFailedThrowable
            }
    }

    @Test
    fun `update wallet config 400 error occurs`() {
        val httpBadRequestException = HttpBadRequestException()
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.error(httpBadRequestException))
        whenever(localWalletConfigProvider.getWalletConfig()).thenReturn(Single.just(localWalletConfigPayload))
        walletManager.masterSeed = MasterSeed()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletManager.updateWalletConfig(WalletConfig())
            .test()
            .assertError {
                it is AutomaticBackupFailedThrowable
            }
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.just(WalletConfigPayload()))
        NetworkManager.initialize(listOf(Network(chainId = 0)))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val throwable = Throwable()
        NetworkManager.initialize(listOf(Network(chainId = 0)))
        whenever(minervaApi.saveWalletConfig(any(), any())).thenReturn(Single.error(throwable))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertSubscribed().awaitCount(1).assertError(throwable)
    }

    @Test
    fun `test slash encryption in public master key for http requests`() {
        val publicKey = CryptoUtils.encodePublicKey(
            "BGQKOB5ZvopzLVObuzLtU/ujTMCvTU/CoX4A/DX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8/uLCWA="
        )
        assertEquals(
            publicKey,
            "BGQKOB5ZvopzLVObuzLtU%2FujTMCvTU%2FCoX4A%2FDX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8%2FuLCWA="
        )
    }

    @Test
    fun `find identity by id test error`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.findIdentityByDid("did:ethr:hehe")
        assertEquals(result?.address, null)
    }

    @Test
    fun `find identity by id test success`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.findIdentityByDid("did:ethr:address")
        assertEquals(result?.address, "address")
    }

    @Test
    fun `get logged in identity by public key success`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.getLoggedInIdentityByPublicKey("publicKey")
        assertEquals(result?.publicKey, "publicKey")
    }

    @Test
    fun `get logged in identity by public key error`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.getLoggedInIdentityByPublicKey("ee")
        assertEquals(result?.publicKey, null)
    }

    @Test
    fun `get value iterator test`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.getValueIterator()
        assertEquals(result, 3)
    }

    @Test
    fun `get safe account number test`() {
        mockWallet()
        walletManager.initWalletConfig()
        val result = walletManager.getSafeAccountNumber("address")
        assertEquals(result, 1)
    }

    @Test
    fun `is mnemonic remembered test`() {
        whenever(localStorage.isMnemonicRemembered()) doReturn true
        val result = walletManager.isMnemonicRemembered()
        assertEquals(result, true)
    }

    @Test
    fun `is not mnemonic remembered test`() {
        whenever(localStorage.isMnemonicRemembered()) doReturn false
        val result = walletManager.isMnemonicRemembered()
        assertEquals(result, false)
    }

    @Test
    fun `save is mnemonic remembered test`() {
        doNothing().whenever(localStorage).saveIsMnemonicRemembered(com.nhaarman.mockitokotlin2.any())
        walletManager.saveIsMnemonicRemembered()
        verify(localStorage, times(1)).saveIsMnemonicRemembered(true)
    }

    @Test
    fun `is synced returns true`() {
        whenever(localStorage.isSynced).thenReturn(true)
        val result = walletManager.isSynced
        assertEquals(true, result)
    }

    @Test
    fun `is synced returns false`() {
        whenever(localStorage.isSynced).thenReturn(false)
        val result = walletManager.isSynced
        assertEquals(false, result)
    }

    @Test
    fun `is backup allowed returns true`() {
        whenever(localStorage.isBackupAllowed).thenReturn(true)
        val result = walletManager.isBackupAllowed
        assertEquals(true, result)
    }

    @Test
    fun `is backup allowed returns false`() {
        whenever(localStorage.isBackupAllowed).thenReturn(false)
        val result = walletManager.isBackupAllowed
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled returns true`() {
        whenever(localStorage.areMainNetworksEnabled).thenReturn(true)
        val result = walletManager.areMainNetworksEnabled
        assertEquals(true, result)
    }

    @Test
    fun `are main nets enabled returns false`() {
        whenever(localStorage.areMainNetworksEnabled).thenReturn(false)
        val result = walletManager.areMainNetworksEnabled
        assertEquals(false, result)
    }

    @Test
    fun `remove all tokens from wallet config success`() {
        val tokenMap = mapOf((1 to listOf(ERCToken(1, type = TokenType.INVALID))))
        mockWallet()
        walletManager.initWalletConfig()
        walletManager.updateWalletConfig(WalletConfig(erc20Tokens = tokenMap)).blockingGet()
        walletManager.removeAllTokens().blockingGet()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
            firstValue.peekContent().erc20Tokens shouldBeEqualTo emptyMap()
        }
    }

    @Test
    fun `remove all tokens from wallet config fail`() {
        val tokenMap = mapOf((1 to listOf(ERCToken(1, type = TokenType.INVALID))))
        mockWallet()
        walletManager.initWalletConfig()
        walletManager.updateWalletConfig(WalletConfig(erc20Tokens = tokenMap)).blockingGet()
        whenever(walletManager.isBackupAllowed).thenReturn(false)
        walletManager.removeAllTokens().blockingGet()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver).onChanged(capture())
            firstValue.peekContent().erc20Tokens shouldBeEqualTo tokenMap
        }
    }
}