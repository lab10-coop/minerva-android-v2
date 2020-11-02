package minerva.android.walletmanager.walletconfig

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.WalletConfigTestValues
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepositoryImpl
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletConfigRepositoryTest : WalletConfigTestValues() {

    private val local = LocalMock()
    private val online = OnlineMock()
    private val onlineLikeLocal = OnlineLikeLocalMock()
    private val localStorage: LocalStorage = mock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val api: MinervaApiRepository = mock()

    private val repository = WalletConfigRepositoryImpl(cryptographyRepository, local, localStorage, onlineLikeLocal)

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

    @Test
    fun `Check that WalletConfig will be updated when online version is different`() {
        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(0)))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))

        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(1)))
            .thenReturn(Single.just(DerivedKeys(1, "publicKey", "privateKey", "address")))

        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(2)))
            .thenReturn(Single.just(DerivedKeys(2, "publicKey", "privateKey", "address")))

        whenever(localStorage.getProfileImage(any())).thenReturn(String.Empty)

        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, local, localStorage, online)
        val observable = walletConfigRepository.getWalletConfig(MasterSeed())
        observable.test().assertValueSequence(listOf(local.prepareWalletConfig(), online.prepareWalletConfig()))
    }

    @Test
    fun `Check that WalletConfig will be updated when online version is the same`() {
        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(0)))
            .thenReturn(Single.just(DerivedKeys(0, "publicKey", "privateKey", "address")))

        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(1)))
            .thenReturn(Single.just(DerivedKeys(1, "publicKey", "privateKey", "address")))

        whenever(cryptographyRepository.computeDeliveredKeys(any(), eq(2)))
            .thenReturn(Single.just(DerivedKeys(2, "publicKey", "privateKey", "address")))

        whenever(localStorage.getProfileImage(any())).thenReturn(String.Empty)


        NetworkManager.initialize(networks)
        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, local, localStorage, onlineLikeLocal)
        val observable = walletConfigRepository.getWalletConfig(MasterSeed())
        observable.test().assertValueSequence(listOf(local.prepareWalletConfig()))
    }

    @Test
    fun `get wallet config when api error occurs`() {
        val error = Throwable()
        val localProvider: LocalWalletConfigProvider = mock()
        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, localProvider, localStorage, api)
        whenever(localProvider.getWalletConfig()).thenReturn(Single.just(WalletConfigPayload(1, listOf(), listOf(), listOf())))
        whenever(api.getWalletConfig(any())).doReturn(Single.error(error))
        walletConfigRepository.getWalletConfig(MasterSeed("seed", "publicKey", "privateKey"))
            .test()
            .assertValue {
                it.version == 1
            }
    }

    @Test
    fun `get wallet config when fetching from local storage error occurs`() {
        val error = Throwable()
        val localProvider: LocalWalletConfigProvider = mock()
        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, localProvider, localStorage, api)
        whenever(localProvider.getWalletConfig()).thenReturn(Single.error(error))
        whenever(api.getWalletConfig(any())).doReturn(
            Single.just(
                WalletConfigResponse(
                    _walletConfigPayload = WalletConfigPayload(
                        1,
                        listOf(),
                        listOf(),
                        listOf()
                    )
                )
            )
        )
        walletConfigRepository.getWalletConfig(MasterSeed("seed", "publicKey", "privateKey"))
            .test()
            .assertValue {
                it.version == 1
            }
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(api.saveWalletConfig(any(), any())).thenReturn(Completable.complete())
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        val test = repository.updateWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val throwable = Throwable()
        val repository = WalletConfigRepositoryImpl(cryptographyRepository, local, localStorage, api)
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        whenever(api.saveWalletConfig(any(), any())).thenReturn(Completable.error(throwable))
        val test = repository.updateWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `test slash encryption in public master key for http requests`() {
        val publicKey = encodePublicKey(
            "BGQKOB5ZvopzLVObuzLtU/ujTMCvTU/CoX4A/DX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8/uLCWA="
        )
        assertEquals(publicKey, "BGQKOB5ZvopzLVObuzLtU%2FujTMCvTU%2FCoX4A%2FDX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8%2FuLCWA=")
    }
}
