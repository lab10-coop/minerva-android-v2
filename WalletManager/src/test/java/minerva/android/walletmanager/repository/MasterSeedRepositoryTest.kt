package minerva.android.walletmanager.repository

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.SeedError
import minerva.android.cryptographyProvider.repository.model.SeedWithKeys
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.MasterSeedError
import minerva.android.walletmanager.repository.seed.MasterSeedRepositoryImpl
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertEquals

class MasterSeedRepositoryTest : RxTest() {

    private val cryptographyRepository: CryptographyRepository = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val repository = MasterSeedRepositoryImpl(walletConfigManager, cryptographyRepository)

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
        whenever(cryptographyRepository.areMnemonicWordsValid(any())) doReturn true
        val result = repository.areMnemonicWordsValid("mnemonic")
        assertEquals(result, true)
    }

    @Test
    fun `restore master seed test`() {
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn SeedWithKeys("seed", "pw", "key2", "key3")
        val result = repository.restoreMasterSeed("mnemonic")
        (result as MasterSeed).apply {
            seed == "seed" &&
                    password == "pw" &&
                    publicKey == "key2" &&
                    privateKey == "key3"
        }
    }

    @Test
    fun `restore master seed error test`() {
        val error = Throwable("Restore seed error")
        whenever(cryptographyRepository.restoreMasterSeed(any())) doReturn SeedError(error)
        val result = repository.restoreMasterSeed("mnemonic")
        (result as MasterSeedError).apply {
            error.message shouldBeEqualTo "Restore seed error"
        }
    }

    @Test
    fun `create master seed  test`() {
        whenever(cryptographyRepository.createMasterSeed()) doReturn Single.just(
            Triple(
                "key1",
                "key2",
                "key3"
            )
        )
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
    fun `create existed master seed test`() {
        val masterSeed = MasterSeed("seed", "k1", "k2")
        whenever(walletConfigManager.createWalletConfig(any())) doReturn Completable.complete()
        repository.createWalletConfig(masterSeed)
            .test()
            .assertNoErrors()
            .assertComplete()
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
        repository.getAccountIterator() shouldBeEqualTo 0
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
}