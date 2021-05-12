package minerva.android.kotlinUtils

import io.mockk.InternalPlatformDsl.toStr
import minerva.android.kotlinUtils.crypto.hexStringToByteArray
import minerva.android.kotlinUtils.crypto.hexToBigInteger
import minerva.android.kotlinUtils.crypto.toByteArray
import minerva.android.kotlinUtils.crypto.toHexString
import org.junit.Test
import java.math.BigDecimal
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

    @Test
    fun `hex to integer test`() {
        val result1 = hexToBigInteger("0xea300b17a8b780000 ", BigDecimal.ZERO)
        assertEquals(result1, BigDecimal("270000000000000000000"))

        val result2 = hexToBigInteger("0x0", BigDecimal.ZERO)
        assertEquals(result2, BigDecimal("0"))

        val result3 = hexToBigInteger("270", BigDecimal.ZERO)
        assertEquals(result3, BigDecimal("270"))

        val result4 = hexToBigInteger("27.32", BigDecimal.ZERO)
        assertEquals(result4, BigDecimal("0"))

        val result5 = hexToBigInteger("0x6f05b59d3b20000", BigDecimal.ZERO)
        assertEquals(result5, BigDecimal("500000000000000000"))

        val result6 = hexToBigInteger("0x16345785D8A0000", BigDecimal.ZERO)
        assertEquals(result6, BigDecimal("100000000000000000"))
    }
}