package minerva.android.kotlinUtils

import io.mockk.InternalPlatformDsl.toStr
import minerva.android.kotlinUtils.crypto.hexStringToByteArray
import minerva.android.kotlinUtils.crypto.toByteArray
import minerva.android.kotlinUtils.crypto.toHexString
import org.junit.Test
import kotlin.test.assertEquals

class CryptoExtensionsTest {

    @Test
    fun `to hex string test`() {
        val result = ByteArray(10).toHexString()
        assertEquals("00000000000000000000", result)
    }

    @Test
    fun `string to byte array test`() {
        val result = "Test".toByteArray().toStr()
        assertEquals("[-2, -17]", result)
    }

    @Test
    fun `hex string to byte array`() {
        val result = hexStringToByteArray("0x123456").toStr()
        assertEquals("[18, 52, 86]", result)
    }
}