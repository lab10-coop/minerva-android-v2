package minerva.android.walletmanager.localstorage

import android.content.SharedPreferences
import io.mockk.*
import minerva.android.walletmanager.model.Recipient
import minerva.android.walletmanager.storage.LocalStorageImpl
import org.junit.Test
import kotlin.test.assertTrue

class LocalStorageTest {

    private val sharedPref = mockk<SharedPreferences> {
        every { edit().putString(any(), any()).apply() } just Runs
        every { edit().putBoolean(any(), any()).apply() } just Runs
        every { getBoolean(any(), any()) } returns true
        every { getString(any(), any()) } returns ""
    }

    private val localStorage = LocalStorageImpl(sharedPref)

    @Test
    fun `is mnemonic remembered test`() {
        localStorage.isMnemonicRemembered()
        verify {
            sharedPref.getBoolean(any(), false)
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `save is mnemonic remembered test`() {
        localStorage.saveIsMnemonicRemembered(true)
        verify {
            sharedPref.edit().putBoolean(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `sync is remembered test`() {
        localStorage.isSynced = true
        verify {
            sharedPref.edit().putBoolean(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `is backup allowed test`() {
        localStorage.isBackupAllowed = true
        verify {
            sharedPref.edit().putBoolean(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `load recipients test`() {
        localStorage.getRecipients()
        verify {
            sharedPref.getString(any(), any())
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `save recipient test`() {
        val recipient = Recipient("name", "address")
        localStorage.saveRecipient(recipient)
        every { localStorage.getRecipients() } returns listOf(recipient)
        verify {
            sharedPref.edit().putString(any(), any()).apply()
            sharedPref.getString(any(), any())
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `save profile image test`() {
        val imageInBase64 = "imageInBase64"
        localStorage.saveProfileImage("name", imageInBase64)
        every { localStorage.getProfileImage(any()) } returns imageInBase64
        verify {
            sharedPref.edit().putString(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `load profile image test`() {
        localStorage.getProfileImage("name")
        verify {
            sharedPref.getString(any(), any())
        }
    }
}