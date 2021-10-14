package minerva.android.walletmanager.keystore

import android.content.SharedPreferences
import android.util.Base64
import io.mockk.*
import minerva.android.walletmanager.model.wallet.MasterSeed
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.SecretKey
import kotlin.test.assertEquals

class KeyStoreRepositoryTest {

    // Workaround for JAVA 11 issue https://github.com/mockk/mockk/issues/280
    interface MockSecretKey : SecretKey

    private val secretKey = mockk<MockSecretKey>()
    private val cipherInstance = mockk<Cipher>()

    private val sharedPref = mockk<SharedPreferences> {
        every { edit().putString(any(), any()).apply() } just Runs
        every { getString(any(), any()) } returns ""
    }

    private val keyStoreManager = mockk<KeyStoreManager>()
    private val keystoreRepositoryImpl = KeystoreRepositoryImpl(sharedPref, keyStoreManager)

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        mockkStatic(Cipher::class)
    }

    @Test
    fun `is mnemonic save test`() {
        every { keyStoreManager.isMasterSeedSaved() } returns true
        every { keyStoreManager.generateSecretKey() } returns secretKey
        every { Cipher.getInstance(any()) } returns cipherInstance
        every { cipherInstance.init(Cipher.ENCRYPT_MODE, secretKey) } just Runs
        every { cipherInstance.doFinal(any()) } returns ByteArray(1)
        every { cipherInstance.iv } returns ByteArray(1)
        every { Base64.encodeToString(any(), any()) } returns "MasterSeed"
        keystoreRepositoryImpl.encryptMasterSeed(MasterSeed())
        val result = keystoreRepositoryImpl.isMasterSeedSaved()
        assertEquals(result, true)
    }

    @Test
    fun `encrypt master seed test`() {
        every { keyStoreManager.isMasterSeedSaved() } returns true
        every { keyStoreManager.generateSecretKey() } returns secretKey
        every { Cipher.getInstance(any()) } returns cipherInstance
        every { cipherInstance.init(Cipher.ENCRYPT_MODE, secretKey) } just Runs
        every { cipherInstance.doFinal(any()) } returns ByteArray(1)
        every { cipherInstance.iv } returns ByteArray(1)
        every { Base64.encodeToString(any(), any()) } returns "MasterSeed"
        keystoreRepositoryImpl.encryptMasterSeed(MasterSeed())
        verify {
            sharedPref.edit().putString(any(), any())
            sharedPref.edit().putString(any(), any())
        }
        confirmVerified(sharedPref)
    }
}