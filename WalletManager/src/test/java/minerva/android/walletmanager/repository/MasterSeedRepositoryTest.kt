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
    private val walletConfigManager: WalletConfigManager = mock()
    private val repository = MasterSeedRepositoryImpl(walletConfigManager, cryptographyRepository)

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
        whenever(walletConfigManager.isMnemonicRemembered()) doReturn true
        val result = repository.isMnemonicRemembered()
        assertEquals(result, true)
    }

    @Test
    fun `is not mnemonic remembered test`() {
        whenever(walletConfigManager.isMnemonicRemembered()) doReturn false
        val result = repository.isMnemonicRemembered()
        assertEquals(result, false)
    }

    @Test
    fun `save is mnemonic remembered test`() {
        doNothing().whenever(walletConfigManager).saveIsMnemonicRemembered()
        repository.saveIsMnemonicRemembered()
        verify(walletConfigManager, times(1)).saveIsMnemonicRemembered()
    }

    @Test
    fun `validate mnemonic test`() {
        whenever(cryptographyRepository.validateMnemonic(any())) doReturn listOf("word")
        val result = repository.validateMnemonic("mnemonic")
        assertEquals(result, listOf("word"))
    }

    @Test
    fun `restore master seed test`() {
        whenever(cryptographyRepository.restoreMasterSeed(any(), any())) doReturn Single.just(Triple("key1", "key2", "key3"))
        whenever(walletConfigManager.restoreWalletConfig(any())) doReturn Completable.complete()
        repository.restoreMasterSeed("mnemonic")
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `restore master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.restoreMasterSeed(any(), any())) doReturn Single.error(error)
        repository.restoreMasterSeed("mnemonic")
            .test()
            .assertError(error)
    }

    @Test
    fun `create master seed  test`() {
        whenever(cryptographyRepository.createMasterSeed(any())) doReturn Single.just(Triple("key1", "key2", "key3"))
        whenever(walletConfigManager.createWalletConfig(any())) doReturn Completable.complete()
        repository.createWalletConfig()
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `create master seed error test`() {
        val error = Throwable()
        whenever(cryptographyRepository.createMasterSeed(any())) doReturn Single.error(error)
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
    fun `get mnemonic test`() {
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

    @Test
    fun `is backup allowed returns true`() {
        whenever(walletConfigManager.isBackupAllowed).thenReturn(true)
        val result = repository.isBackupAllowed
        assertEquals(true, result)
    }

    @Test
    fun `is backup allowed returns false`() {
        whenever(walletConfigManager.isBackupAllowed).thenReturn(false)
        val result = repository.isBackupAllowed
        assertEquals(false, result)
    }

    @Test
    fun `is synced returns true`() {
        whenever(walletConfigManager.isSynced).thenReturn(true)
        val result = repository.isSynced
        assertEquals(true, result)
    }

    @Test
    fun `is synced returns false`() {
        whenever(walletConfigManager.isSynced).thenReturn(false)
        val result = repository.isSynced
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled returns false`() {
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(false)
        val result = repository.areMainNetworksEnabled
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled returns true`() {
        whenever(walletConfigManager.areMainNetworksEnabled).thenReturn(true)
        val result = repository.areMainNetworksEnabled
        assertEquals(true, result)
    }

    @Test
    fun `toggle main nets enabled returns true`() {
        whenever(walletConfigManager.toggleMainNetsEnabled).thenReturn(true)
        val result = repository.toggleMainNetsEnabled
        assertEquals(true, result)
    }
}