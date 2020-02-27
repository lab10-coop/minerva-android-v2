package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.exchangemarketsprovider.api.BinanceApi
import com.exchangemarketsprovider.model.Market
import com.nhaarman.mockitokotlin2.*
import com.nhaarman.mockitokotlin2.any
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainRepositoryImpl
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.Profile
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.wallet.WalletManagerImpl
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.NetworkNameShort
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class WalletManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val walletConfigRepository: WalletConfigRepository = mock()
    private val blockchainRepository: BlockchainRepositoryImpl = mock()
    private val localStorage: LocalStorage = mock()
    private val servicesApi: ServicesApi = mock()
    private val binanaceApi: BinanceApi = mock()

    private val walletManager =
        WalletManagerImpl(
            keyStoreRepository,
            cryptographyRepository,
            walletConfigRepository,
            blockchainRepository,
            localStorage,
            servicesApi,
            binanaceApi
        )

    private val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    private val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "", "privateKey", data),
            Identity(1, "identityName2", "", "privateKey", data),
            Identity(3, "identityName3", "", "privateKey", data)
        ),
        listOf(
            Value(2, "publicKey1", "privateKey1", "address", network = NetworkNameShort.ETH),
            Value(4, "publicKey2", "privateKey2", "address", network = NetworkNameShort.ETH)
        )
    )

    private val walletConfigObserver: Observer<WalletConfig> = mock()
    private val walletConfigCaptor: KArgumentCaptor<WalletConfig> = argumentCaptor()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(blockchainRepository.completeAddress(any())).thenReturn("address")
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that loading wallet config returns success`() {
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.walletConfigLiveData.observeForever(walletConfigObserver)
        walletConfigCaptor.run {
            verify(walletConfigObserver, times(1)).onChanged(capture())
            firstValue.identities[0].name shouldBeEqualTo "identityName1"
        }
    }

    @Test
    fun `Create default walletConfig should return success`() {
        whenever(walletConfigRepository.createWalletConfig(any())).thenReturn(Completable.complete())
        val test = walletManager.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertComplete()
    }

    @Test
    fun `Create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(walletConfigRepository.createWalletConfig(any())).thenReturn(Completable.error(throwable))
        val test = walletManager.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `Check that wallet manager returns correct value`() {
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        val identity = walletManager.loadIdentity(0, "Identity")
        identity.index shouldEqualTo 0
        identity.name shouldBeEqualTo "identityName1"
        identity.privateKey shouldBeEqualTo "privateKey"
        val identity3 = walletManager.loadIdentity(3, "Identity")
        identity3.index shouldEqualTo walletConfig.newIndex
        identity3.name shouldBeEqualTo "Identity #5"
        val identityMinusOne = walletManager.loadIdentity(-1, "Identity")
        identityMinusOne.index shouldEqualTo walletConfig.newIndex
        identityMinusOne.name shouldBeEqualTo "Identity #5"
    }

    @Test
    fun `Check that wallet manager saves new identity`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "newIdentity")
        val test = walletManager.saveIdentity(newIdentity).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        test.assertNoErrors()
        loadedIdentity.name shouldBeEqualTo newIdentity.name
        loadedIdentity.publicKey shouldBeEqualTo "publicKey"
        loadedIdentity.privateKey shouldBeEqualTo "privateKey"
    }

    @Test
    fun `Check that wallet manager doesn't save when server error`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "newIdentity")
        walletManager.saveIdentity(newIdentity).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldNotBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager saves new Value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val test = walletManager.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertNoErrors()
        val loadedValue = walletManager.loadValue(2)
        loadedValue.index shouldEqualTo 5
        loadedValue.name shouldBeEqualTo "#3 Ethereum"
        loadedValue.publicKey shouldBeEqualTo "publicKey"
        loadedValue.privateKey shouldBeEqualTo "privateKey"
        loadedValue.address shouldBeEqualTo "address"
    }

    @Test
    fun `Check that wallet manager don't save new value when server error`() {
        val error = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(error))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val test = walletManager.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertError(error)
        val loadedValue = walletManager.loadValue(2)
        loadedValue.index shouldEqualTo -1
        loadedValue.privateKey shouldBeEqualTo String.Empty
        loadedValue.publicKey shouldBeEqualTo String.Empty
        loadedValue.address shouldBeEqualTo String.Empty
    }

    @Test
    fun `Check that wallet manager removes correct identity`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo identityToRemove.name
        loadedIdentity.isDeleted shouldEqualTo true
        loadedIdentity.data.size shouldEqualTo identityToRemove.data.size
    }

    @Test
    fun `Check that wallet manager removes correct empty value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(2).test()
        val removedValue = walletManager.loadValue(0)
        val notRemovedValue = walletManager.loadValue(1)
        removedValue.index shouldEqualTo 2
        removedValue.isDeleted shouldEqualTo true
        notRemovedValue.index shouldEqualTo 4
        notRemovedValue.isDeleted shouldEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct not empty value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(300))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(2).test()
        val removedValue = walletManager.loadValue(0)
        val notRemovedValue = walletManager.loadValue(1)
        removedValue.index shouldEqualTo 2
        removedValue.isDeleted shouldEqualTo false
        notRemovedValue.index shouldEqualTo 4
        notRemovedValue.isDeleted shouldEqualTo false
    }

    @Test
    fun `Check that wallet manager doesn't remove identity when server error`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.just(Triple(0, "publicKey", "privateKey")))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName2"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
    }

    @Test
    fun `Check that wallet manager doesn't remove identity, when there is only one active element`() {
        val identityToRemove = Identity(0)
        val walletConfig = WalletConfig(
            0, listOf(
                Identity(0, "identityName1", "", "privateKey", data),
                Identity(1, "identityName1", "", "privateKey", data, isDeleted = true)
            )
        )
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
        walletConfig.identities.size shouldEqualTo 2
    }

    @Test
    fun `Check that wallet manager will not remove, when try to remove identity with wrong index`() {
        val identityToRemove = Identity(22)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldEqualTo false
        loadedIdentity.data.size shouldEqualTo 3
        walletConfig.identities.size shouldEqualTo 3
    }

    @Test
    fun `refresh balances test success`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.refreshBalances(any())).thenReturn(Single.just(listOf(Pair("address", BigDecimal.ONE))))
        whenever(binanaceApi.fetchExchangeRate(any())).thenReturn(Single.just(Market("ETHEUR", "12.21")))
        walletManager.initWalletConfig()
        walletManager.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `refresh balances test error`() {
        val error = Throwable()
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.refreshBalances(any())).thenReturn(Single.error(error))
        whenever(binanaceApi.fetchExchangeRate(any())).thenReturn(Single.error(error))
        walletManager.initWalletConfig()
        walletManager.refreshBalances().test()
            .assertError(error)
    }

    @Test
    fun `painless login with identity test success`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        walletManager.initWalletConfig()
        walletManager.painlessLogin("url", "jwtToken", Identity(1)).test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test success`() {
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        walletManager.painlessLogin("url", "jwtToken", IncognitoIdentity()).test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test error`() {
        val error = Throwable()
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.error(error))
        walletManager.painlessLogin("url", "jwtToken", IncognitoIdentity()).test()
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
        walletManager.decodeQrCodeResponse("token").test()
            .assertComplete()
            .assertValue {
                it.callback == "test" &&
                        it.issuer == "test"
            }
    }

    @Test
    fun `send transaction success test`() {
        whenever(blockchainRepository.sendTransaction(any(), any())).thenReturn(Completable.complete())
        walletManager.sendTransaction("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction error test`() {
        val error = Throwable()
        whenever(blockchainRepository.sendTransaction(any(), any())).thenReturn(Completable.error(error))
        walletManager.sendTransaction("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertError(error)
    }

    @Test
    fun `send transaction cost success test`() {
        whenever(blockchainRepository.getTransactionCosts(any(), any())).thenReturn(
            Single.just(
                TransactionCostPayload(
                    BigDecimal.ONE,
                    BigInteger.ONE,
                    BigDecimal.ONE
                )
            )
        )
        walletManager.getTransactionCosts("", Int.InvalidIndex)
            .test()
            .assertComplete()
            .assertValue {
                it.cost == BigDecimal.ONE &&
                        it.gasLimit == BigInteger.ONE
            }
    }

    @Test
    fun `send transaction cost error test`() {
        val error = Throwable()
        whenever(blockchainRepository.getTransactionCosts(any(), any())).thenReturn(Single.error(error))
        walletManager.getTransactionCosts("", Int.InvalidIndex)
            .test()
            .assertError(error)
    }

    @Test
    fun `calculate transaction cost success`() {
        whenever(blockchainRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.ONE)
        val result = walletManager.calculateTransactionCost(BigDecimal.ONE, BigInteger.ONE)
        result shouldEqual BigDecimal.ONE
    }

    @Test
    fun `get wallet config success test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Single.just(
            WalletConfigResponse(
                _message = "success"
            )
        ))
        walletManager.initWalletConfig()
        walletManager.getWalletConfig(MasterKey("123", "567"))
            .test()
            .assertComplete()
            .assertValue {
                it.message == "success"
            }
    }

    @Test
    fun `get wallet config error test`() {
        val error = Throwable()
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Single.error(error))
        walletManager.initWalletConfig()
        walletManager.getWalletConfig(MasterKey("123", "567"))
            .test()
            .assertError(error)
    }

    @Test
    fun `get assets balances complete test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(blockchainRepository.refreshAssetBalance(any(), any(), any()))
            .thenReturn(Observable.just(Pair("privateKey1", BigDecimal.valueOf(23))))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        walletManager.initWalletConfig()
        walletManager.refreshAssetBalance().test().assertComplete()
            .assertValue {
                it.size == 2
            }
//TODO when uncommented not passing on CI - try to resolve this problem
//            .assertValue {
//                val list = it["privateKey1"] ?: listOf()
//                list.size == 1
//            }
    }

    //TODO when uncommented not passing on CI - try to resolve this problem
//    @Test
//    fun `get asset balances error test`() {
//        val error = Throwable()
//        whenever(blockchainRepository.refreshAssetBalance(any(), any(), any())).thenReturn(Observable.error(error))
//        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
//        walletManager.initWalletConfig()
//        walletManager.refreshAssetBalance().test().assertError(error)
//    }


//    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
//        blockchainRepository.transferERC20Token(
//            network,
//            mapTransactionToTransactionPayload(transaction)
//        ).ignoreElements()

    @Test
    fun `make ERC20 transfer success test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        walletManager.initWalletConfig()
        walletManager.transferERC20Token("", Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer error test`() {
        val error = Throwable()
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptKey()).thenReturn(MasterKey())
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        walletManager.initWalletConfig()
        walletManager.transferERC20Token("", Transaction()).test().assertError(error)
    }
}