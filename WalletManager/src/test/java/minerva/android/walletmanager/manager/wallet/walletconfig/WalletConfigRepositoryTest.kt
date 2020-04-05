package minerva.android.walletmanager.manager.wallet.walletconfig

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepositoryImpl
import minerva.android.configProvider.api.MinervaApi
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepositoryImpl
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletConfigRepositoryTest {

    private val local = LocalMock()
    private val online = OnlineMock()
    private val onlineLikeLocal = OnlineLikeLocalMock()
    private val cryptographyRepository: CryptographyRepository = mock()
    private val api: MinervaApi = mock()

    private val repository = WalletConfigRepositoryImpl(cryptographyRepository, local, onlineLikeLocal)

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

        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, local, online)
        val observable = walletConfigRepository.loadWalletConfig(MasterSeed())
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

        val walletConfigRepository = WalletConfigRepositoryImpl(cryptographyRepository, local, onlineLikeLocal)
        val observable = walletConfigRepository.loadWalletConfig(MasterSeed())
        observable.test().assertValueSequence(listOf(local.prepareWalletConfig()))
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.complete())
        val test = repository.createWalletConfig(MasterSeed("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val throwable = Throwable()
        val repository = WalletConfigRepositoryImpl(cryptographyRepository, local, api)
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.error(throwable))
        val test = repository.createWalletConfig(MasterSeed("1234", "5678")).test()
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
