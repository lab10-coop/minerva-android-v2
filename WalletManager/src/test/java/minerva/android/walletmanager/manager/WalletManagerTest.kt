package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.exchangemarketsprovider.api.BinanceApi
import com.exchangemarketsprovider.model.Market
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.LoginResponse
import minerva.android.servicesApiProvider.model.Profile
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.PaymentRequest
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.wallet.WalletManagerImpl
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class WalletManagerTest {

    private val keyStoreRepository: KeystoreRepository = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val walletConfigRepository: WalletConfigRepository = mock()
    private val blockchainRepository: BlockchainRepository = mock()
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
            Value(2, "publicKey1", "privateKey1", "address", network = NetworkShortName.ETH),
            Value(4, "publicKey2", "privateKey2", "address", network = NetworkShortName.ATS),
            Value(
                5, "publicKey3", "privateKey3", "address", network = NetworkShortName.ATS,
                owners = listOf("masterOwner")
            ),
            Value(
                6, "publicKey4", "privateKey4", "address", network = NetworkShortName.ATS,
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Value(7, "publicKey5", "privateKey5", "address", network = NetworkShortName.ATS)
        )
    )

    private val walletConfig2 = WalletConfig(
        0, listOf(),
        listOf(
            Value(2, "publicKey11", "privateKey1", "address", network = NetworkShortName.ETH),
            Value(4, "publicKey22", "privateKey2", "address", network = NetworkShortName.ETH),
            Value(
                5, "publicKey33", "privateKey3", "masterOwner", network = NetworkShortName.ETH,
                owners = listOf("masterOwner")
            ),
            Value(
                6, "publicKey44", "privateKey4", "address", network = NetworkShortName.ETH,
                owners = listOf("notMasterOwner", "masterOwner")
            )
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
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `Check that loading wallet config returns success`() {
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
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
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertComplete()
    }

    @Test
    fun `Create default walletConfig should return error`() {
        val throwable = Throwable()
        whenever(walletConfigRepository.createWalletConfig(any())).thenReturn(Completable.error(throwable))
        val test = walletManager.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `Check that wallet manager returns correct value`() {
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        val identity = walletManager.loadIdentity(0, "Identity")
        identity.index shouldBeEqualTo 0
        identity.name shouldBeEqualTo "identityName1"
        identity.privateKey shouldBeEqualTo "privateKey"
        val identity3 = walletManager.loadIdentity(3, "Identity")
        identity3.index shouldBeEqualTo walletConfig.newIndex
        identity3.name shouldBeEqualTo "Identity #8"
        val identityMinusOne = walletManager.loadIdentity(-1, "Identity")
        identityMinusOne.index shouldBeEqualTo walletConfig.newIndex
        identityMinusOne.name shouldBeEqualTo "Identity #8"
    }

    @Test
    fun `Check that wallet manager saves new identity`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "identityName1")
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
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val newIdentity = Identity(0, "identityName")
        walletManager.saveIdentity(newIdentity).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldNotBeEqualTo newIdentity.name
    }

    @Test
    fun `Check that wallet manager saves new Value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val test = walletManager.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertNoErrors()
        val loadedValue = walletManager.loadValue(1)
        loadedValue.index shouldBeEqualTo 4
        loadedValue.publicKey shouldBeEqualTo "publicKey2"
        loadedValue.privateKey shouldBeEqualTo "privateKey2"
        loadedValue.address shouldBeEqualTo "address"
    }

    @Test
    fun `Check that wallet manager don't save new value when server error`() {
        val error = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(error))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        val test = walletManager.createValue(Network.ETHEREUM, "#3 Ethereum").test()
        test.assertError(error)
        val loadedValue = walletManager.loadValue(10)
        loadedValue.index shouldBeEqualTo -1
        loadedValue.privateKey shouldBeEqualTo String.Empty
        loadedValue.publicKey shouldBeEqualTo String.Empty
        loadedValue.address shouldBeEqualTo String.Empty
    }

    @Test
    fun `Check that wallet manager removes correct identity`() {
        val identityToRemove = Identity(1, "identityName2", isDeleted = true)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo identityToRemove.name
        loadedIdentity.isDeleted shouldBeEqualTo identityToRemove.isDeleted
    }

    @Test
    fun `Check that wallet manager removes correct empty value`() {
        val identityToRemove = Identity(4, "identityName2", isDeleted = true)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(2).test()
        val removedValue = walletManager.loadValue(0)
        val notRemovedValue = walletManager.loadValue(1)
        removedValue.index shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo true
        notRemovedValue.index shouldBeEqualTo 4
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct not empty value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(300))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(2).test()
        val removedValue = walletManager.loadValue(0)
        val notRemovedValue = walletManager.loadValue(1)
        removedValue.index shouldBeEqualTo 2
        removedValue.isDeleted shouldBeEqualTo false
        notRemovedValue.index shouldBeEqualTo 4
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager don't removes correct safe account value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(5).test()
        walletManager.removeValue(6).test()
        val notRemovedValue = walletManager.loadValue(2)
        val notRemovedValue2 = walletManager.loadValue(3)
        notRemovedValue.index shouldBeEqualTo 5
        notRemovedValue.publicKey shouldBeEqualTo "publicKey3"
        notRemovedValue.isDeleted shouldBeEqualTo false
        notRemovedValue2.index shouldBeEqualTo 6
        notRemovedValue2.publicKey shouldBeEqualTo "publicKey4"
        notRemovedValue2.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager removes correct safe account value`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig2))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.toGwei(any())).thenReturn(BigInteger.valueOf(256))
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeValue(5).test()
        walletManager.removeValue(6).test()
        val removedValue = walletManager.loadValue(2)
        val notRemovedValue = walletManager.loadValue(3)
        removedValue.index shouldBeEqualTo 5
        removedValue.publicKey shouldBeEqualTo "publicKey33"
        removedValue.isDeleted shouldBeEqualTo true
        notRemovedValue.index shouldBeEqualTo 6
        notRemovedValue.publicKey shouldBeEqualTo "publicKey44"
        notRemovedValue.isDeleted shouldBeEqualTo false
    }

    @Test
    fun `Check that wallet manager doesn't remove identity when server error`() {
        val identityToRemove = Identity(1)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(1, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName2"
        loadedIdentity.isDeleted shouldBeEqualTo false
        loadedIdentity.data.size shouldBeEqualTo 3
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
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldBeEqualTo false
        loadedIdentity.data.size shouldBeEqualTo 3
        walletConfig.identities.size shouldBeEqualTo 2
    }

    @Test
    fun `Check that wallet manager will not remove, when try to remove identity with wrong index`() {
        val identityToRemove = Identity(22)
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.removeIdentity(identityToRemove).test()
        val loadedIdentity = walletManager.loadIdentity(0, "Identity")
        loadedIdentity.name shouldBeEqualTo "identityName1"
        loadedIdentity.isDeleted shouldBeEqualTo false
        loadedIdentity.data.size shouldBeEqualTo 3
        walletConfig.identities.size shouldBeEqualTo 3
    }

    @Test
    fun `refresh balances test success`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
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
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
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
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        walletManager.initWalletConfig()
        walletManager.painlessLogin("url", "jwtToken", Identity(1), Service("1")).test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test success`() {
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.just(LoginResponse(Profile("did:123"))))
        walletManager.painlessLogin("url", "jwtToken", IncognitoIdentity(), Service("1")).test()
            .assertComplete()
    }

    @Test
    fun `painless login with incognito identity test error`() {
        val error = Throwable()
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(servicesApi.painlessLogin(any(), any(), any())).thenReturn(Single.error(error))
        walletManager.painlessLogin("url", "jwtToken", IncognitoIdentity(), Service("1")).test()
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
                it.callback == "test" && it.issuer == "test"
            }
    }

    @Test
    fun `send transaction success with resolved ENS test`() {
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        walletManager.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction success with not resolved ENS test`() {
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable("No ENS")))
        walletManager.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction error test`() {
        val error = Throwable()
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Completable.error(error))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable()))
        walletManager.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertError(error)
    }

    @Test
    fun `calculate transaction cost success`() {
        whenever(blockchainRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.ONE)
        val result = walletManager.calculateTransactionCost(BigDecimal.ONE, BigInteger.ONE)
        result shouldBeEqualTo BigDecimal.ONE
    }

    @Test
    fun `get wallet config success test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(
            Single.just(
                WalletConfigResponse(
                    _message = "success"
                )
            )
        )
        walletManager.initWalletConfig()
        walletManager.getWalletConfig(MasterSeed("123", "567"))
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
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(walletConfigRepository.getWalletConfig(any())).thenReturn(Single.error(error))
        walletManager.initWalletConfig()
        walletManager.getWalletConfig(MasterSeed("123", "567"))
            .test()
            .assertError(error)
    }

    @Test
    fun `get assets balances complete test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(blockchainRepository.refreshAssetBalance(any(), any(), any(), any()))
            .thenReturn(Observable.just(Pair("privateKey1", BigDecimal.valueOf(23))))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.refreshAssetBalance().test().assertComplete()
            .assertValue {
                it.size == 5
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
//        whenever(keyStoreRepository.decryptKey()).thenReturn(minerva.android.walletmanager.model.MasterSeed())
//        walletManager.initWalletConfig()
//        walletManager.refreshAssetBalance().test().assertError(error)
//    }

//    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
//        blockchainRepository.transferERC20Token(
//            network,
//            mapTransactionToTransactionPayload(transaction)
//        ).ignoreElements()

    @Test
    fun `make ERC20 transfer with ENS resolved success test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        walletManager.initWalletConfig()
        walletManager.transferERC20Token("", Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer with not ENS resolved success test`() {
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable()))
        walletManager.initWalletConfig()
        walletManager.transferERC20Token("", Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer error test`() {
        val error = Throwable()
        whenever(walletConfigRepository.loadWalletConfig(any())).thenReturn(Observable.just(walletConfig))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        walletManager.initWalletConfig()
        walletManager.transferERC20Token("", Transaction()).test().assertError(error)
    }

    @Test
    fun `save service test`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(
            Single.just(
                DerivedKeys(
                    0,
                    "publicKey",
                    "privateKey",
                    "address"
                )
            )
        )
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.initWalletConfig()
        walletManager.loadWalletConfig()
        walletManager.saveService(Service()).test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save service test error`() {
        val error = Throwable()
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.error(error))
        whenever(cryptographyRepository.computeDeliveredKeys(any(), any())).thenReturn(Single.error(error))
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.apply {
            initWalletConfig()
            loadWalletConfig()
            saveService(Service()).test().assertError(error)
        }
    }

    @Test
    fun `get correct new Value number`() {
        whenever(walletConfigRepository.updateWalletConfig(any(), any())).thenReturn(Completable.complete())
        whenever(keyStoreRepository.decryptMasterSeed()).thenReturn(MasterSeed())
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            getValueIterator() shouldBeEqualTo 4
        }
    }

    @Test
    fun `create jwtToken success test`() {
        whenever(cryptographyRepository.createJwtToken(any(), any())) doReturn Single.just("token")
        walletManager.createJwtToken(mapOf("name" to "tom"), "privateKey")
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
        walletManager.createJwtToken(mapOf("name" to "tom"), "privateKey")
            .test()
            .assertError(error)
    }

    @Test
    fun `decode payment token success test`() {
        val jwtData = mapOf<String, Any?>(
            PaymentRequest.AMOUNT to "amount", PaymentRequest.IBAN to "iban",
            PaymentRequest.RECIPIENT to "recipient", PaymentRequest.SERVICE_NAME to "name", PaymentRequest.SERVICE_SHORT_NAME to "short",
            PaymentRequest.URL to "url"
        )
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.just(jwtData)
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            decodePaymentRequestToken("jwtToken")
                .test()
                .assertComplete()
                .assertNoErrors()
        }
    }

    @Test
    fun `decode payment token error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.error(error)
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            decodePaymentRequestToken("jwtToken")
                .test()
                .assertError(error)
        }
    }

    @Test
    fun `decode qr code response success test`() {
        val jwtData = mapOf<String, Any?>(PaymentRequest.URL to "url", "iss" to "123", "requested" to arrayListOf<String>("test"))
        whenever(cryptographyRepository.decodeJwtToken(any())) doReturn Single.just(jwtData)
        walletManager.decodeQrCodeResponse("token")
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
        walletManager.decodeQrCodeResponse("token")
            .test()
            .assertError(error)
    }

    @Test
    fun `get logged in identity test`() {
        val expected = Identity(0, name = "tom", publicKey = "publicKey")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(identities = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getLoggedInIdentity("publicKey")
            assertEquals(result, expected)
        }
    }

    @Test
    fun `get logged in identity error test`() {
        val expected = Identity(0, name = "tom")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(identities = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getLoggedInIdentity("publicKey")
            assertEquals(result, null)
        }
    }

    @Test
    fun `get logged in identity public key test`() {
        val expected = Service("iss", name = "tom", loggedInIdentityPublicKey = "key")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(services = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getLoggedInIdentityPublicKey("iss")
            assertEquals(result, expected.loggedInIdentityPublicKey)
        }
    }

    @Test
    fun `get logged in identity public key error test`() {
        val expected = Service("iss", name = "tom")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(services = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getLoggedInIdentityPublicKey("iss")
            assertEquals(result, "")
        }
    }

    @Test
    fun `get safe account master owner key test`() {
        val expected = Value(0, address = "address", privateKey = "key")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "key")
        }
    }

    @Test
    fun `get safe account master owner key error test`() {
        val expected = Value(0, address = "123", privateKey = "key")
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "")
        }
    }

    @Test
    fun `is already logged in test`() {
        val expected = Service(type = ServiceType.CHARGING_STATION)
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(services = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = isAlreadyLoggedIn(ServiceType.CHARGING_STATION)
            assertEquals(result, true)
        }
    }

    @Test
    fun `is already logged in error test`() {
        val expected = Service(type = ServiceType.CHARGING_STATION)
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(services = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = isAlreadyLoggedIn("issuer")
            assertEquals(result, false)
        }
    }

    @Test
    fun `get safe account number test`() {
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("owner"))
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getSafeAccountNumber("owner")
            assertEquals(result, 2)
        }
    }

    @Test
    fun `get safe account number error test`() {
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            val result = getSafeAccountNumber("owner")
            assertEquals(result, 1)
        }
    }

    @Test
    fun `resolve ens test`() {
        whenever(blockchainRepository.resolveENS(any())) doReturn Single.just("tom")
        walletManager.resolveENS("tom.eth")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `resolve ens error test`() {
        val error = Throwable()
        whenever(blockchainRepository.resolveENS(any())) doReturn Single.error(error)
        walletManager.resolveENS("tom.eth")
            .test()
            .assertError(error)
    }

    @Test
    fun `load recipients test`() {
        whenever(localStorage.loadRecipients()) doReturn listOf(Recipient(ensName = "tom"))
        val result = walletManager.loadRecipients()
        assertEquals(result, listOf(Recipient(ensName = "tom")))
    }

    @Test
    fun `get transfer costs test`() {
        whenever(blockchainRepository.getTransactionCosts(any(), any(), any())) doReturn TransactionCostPayload(
            BigDecimal.ONE,
            BigInteger.ONE,
            BigDecimal.ONE
        )
        val result = walletManager.getTransferCosts("network", 1)
        assertEquals(result, TransactionCost(BigDecimal.ONE, BigInteger.ONE, BigDecimal.ONE))
    }

    @Test
    fun `update safe account owners test`() {
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())) doReturn Completable.complete()
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            updateSafeAccountOwners(0, listOf("ownerAddress"))
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue {
                    it[0] == "ownerAddress"
                }
        }
    }

    @Test
    fun `update safe account owners error test`() {
        val error = Throwable()
        val expected = Value(0, address = "123", privateKey = "key", owners = listOf("ownerAddress"))
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed()
        whenever(walletConfigRepository.loadWalletConfig(any())) doReturn Observable.just(WalletConfig(values = listOf(expected)))
        whenever(walletConfigRepository.updateWalletConfig(any(), any())) doReturn Completable.error(error)
        walletManager.run {
            initWalletConfig()
            loadWalletConfig()
            updateSafeAccountOwners(0, listOf("ownerAddress"))
                .test()
                .assertError(error)
        }
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
        doNothing().whenever(localStorage).saveIsMnemonicRemembered(any())
        walletManager.saveIsMnemonicRemembered()
        verify(localStorage, times(1)).saveIsMnemonicRemembered(true)
    }

    @Test
    fun `get mnemonic test`() {
        whenever(cryptographyRepository.getMnemonicForMasterSeed(any())) doReturn "mnemonic"
        whenever(keyStoreRepository.decryptMasterSeed()) doReturn MasterSeed(_seed = "seed")
        val result = walletManager.getMnemonic()
        assertEquals(result, "mnemonic")
    }

    @Test
    fun `validate mnemonic test`() {
        whenever(cryptographyRepository.validateMnemonic(any())) doReturn listOf("word")
        val result = walletManager.validateMnemonic("mnemonic")
        assertEquals(result, listOf("word"))
    }

    @Test
    fun `restore master seed test`() {
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn Single.just(Triple("key1", "key2", "key3"))
        walletManager.restoreMasterSeed("mnemonic")
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.seed == "key1"
            }
    }

    @Test
    fun `restore master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn Single.error(error)
        walletManager.restoreMasterSeed("mnemonic")
            .test()
            .assertError(error)
    }

    @Test
    fun `create master seed  test`() {
        whenever(cryptographyRepository.createMasterSeed()) doReturn Single.just(Triple("key1", "key2", "key3"))
        walletManager.createMasterSeed()
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.seed == "key1"
            }
    }

    @Test
    fun `create master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.createMasterSeed()) doReturn Single.error(error)
        walletManager.createMasterSeed()
            .test()
            .assertError(error)
    }

    @Test
    fun `is master seed available test`() {
        whenever(keyStoreRepository.isMasterSeedSaved()) doReturn true
        val result = walletManager.isMasterSeedAvailable()
        assertEquals(result, true)
    }

    @Test
    fun `master is not seed available test`() {
        whenever(keyStoreRepository.isMasterSeedSaved()) doReturn false
        val result = walletManager.isMasterSeedAvailable()
        assertEquals(result, false)
    }
}