package minerva.android.walletmanager.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.RestoreWalletResponse
import minerva.android.walletmanager.repository.seed.MasterSeedRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class MasterSeedRepositoryTest {

    private val cryptographyRepository: CryptographyRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val repository = MasterSeedRepositoryImpl(walletConfigManager, localStorage, cryptographyRepository)

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

    @Test
    fun `is mnemonic remembered test`() {
        whenever(localStorage.isMnemonicRemembered()) doReturn true
        val result = repository.isMnemonicRemembered()
        assertEquals(result, true)
    }

    @Test
    fun `is not mnemonic remembered test`() {
        whenever(localStorage.isMnemonicRemembered()) doReturn false
        val result = repository.isMnemonicRemembered()
        assertEquals(result, false)
    }

    @Test
    fun `save is mnemonic remembered test`() {
        doNothing().whenever(localStorage).saveIsMnemonicRemembered(any())
        repository.saveIsMnemonicRemembered()
        verify(localStorage, times(1)).saveIsMnemonicRemembered(true)
    }

    @Test
    fun `validate mnemonic test`() {
        whenever(cryptographyRepository.validateMnemonic(any())) doReturn listOf("word")
        val result = repository.validateMnemonic("mnemonic")
        assertEquals(result, listOf("word"))
    }

    @Test
    fun `restore master seed test`() {
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn Single.just(Triple("key1", "key2", "key3"))
        whenever(walletConfigManager.restoreWalletConfig(any())) doReturn Single.just(RestoreWalletResponse(message = "test"))
        repository.restoreMasterSeed("mnemonic")
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.message == "test"
            }
    }

    @Test
    fun `restore master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn Single.error(error)
        repository.restoreMasterSeed("mnemonic")
            .test()
            .assertError(error)
    }

    @Test
    fun `create master seed  test`() {
        whenever(cryptographyRepository.createMasterSeed()) doReturn Single.just(Triple("key1", "key2", "key3"))
        whenever(walletConfigManager.createWalletConfig(any())) doReturn Completable.complete()
        repository.createWalletConfig()
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `create master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.createMasterSeed()) doReturn Single.error(error)
        repository.createWalletConfig()
            .test()
            .assertError(error)
    }

    @Test
    fun `is master seed available test`() {
        whenever(walletConfigManager.isMasterSeedSaved()) doReturn true
        val result = repository.isMasterSeedAvailable()
        assertEquals(result, true)
    }

    @Test
    fun `master is not seed available test`() {
        whenever(walletConfigManager.isMasterSeedSaved()) doReturn false
        val result = repository.isMasterSeedAvailable()
        assertEquals(result, false)
    }

    @Test
    fun `get mnemonic test`(){
        whenever(cryptographyRepository.getMnemonicForMasterSeed(any())) doReturn "mnemonic"
        whenever(walletConfigManager.masterSeed) doReturn MasterSeed(_seed = "seed")
        val result = repository.getMnemonic()
        assertEquals(result, "mnemonic")
    }

    @Test
    fun `get correct new Value number`() {
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        doNothing().whenever(walletConfigManager).initWalletConfig()
        repository.getValueIterator() shouldBeEqualTo 0
    }
}