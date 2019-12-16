package minerva.android

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepositoryImpl
import org.junit.Test
import kotlin.test.assertEquals

class CryptographyRepositoryTest {

    private val context: Context = mock()
    private val repository: CryptographyRepository = CryptographyRepositoryImpl(context)

    @Test
    fun `test mnemonic validator`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator collecting invalid words`() {
        val mnemonic = "vessel *$ alter error federal HEHE chat ability sun Test valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, listOf("*$", "HEHE", "Test"))
    }

    @Test
    fun `test mnemonic validator when mnemonic is empty`() {
        val mnemonic = ""
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator when mnemonic has blank spaces`() {
        val mnemonic = "     "
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, emptyList())
    }

    @Test
    fun `test mnemonic validator when mnemonic is too short and has invalid words`() {
        val mnemonic = "vessel error federal aaaaa sibling chat ability kkkkk sun glass valve picture"
        val validation = repository.validateMnemonic(mnemonic)
        assertEquals(validation, listOf("aaaaa", "kkkkk"))
    }

}