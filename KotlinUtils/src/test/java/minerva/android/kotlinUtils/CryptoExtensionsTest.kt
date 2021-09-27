package minerva.android.kotlinUtils

import io.mockk.InternalPlatformDsl.toStr
import minerva.android.kotlinUtils.crypto.*
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        val result1 = hexToBigDecimal("0xea300b17a8b780000 ", BigDecimal.ZERO)
        assertEquals(result1, BigDecimal("270000000000000000000"))

        val result2 = hexToBigDecimal("0x0", BigDecimal.ZERO)
        assertEquals(result2, BigDecimal("0"))

        val result3 = hexToBigDecimal("270", BigDecimal.ZERO)
        assertEquals(result3, BigDecimal("270"))

        val result4 = hexToBigDecimal("27.32", BigDecimal.ZERO)
        assertEquals(result4, BigDecimal("0"))

        val result5 = hexToBigDecimal("0x6f05b59d3b20000", BigDecimal.ZERO)
        assertEquals(result5, BigDecimal("500000000000000000"))

        val result6 = hexToBigDecimal("0x16345785D8A0000", BigDecimal.ZERO)
        assertEquals(result6, BigDecimal("100000000000000000"))

         val result7 = hexToBigDecimal("0xDE0B6B3A7640000", BigDecimal.ZERO)
        assertEquals(result7, BigDecimal("1000000000000000000"))

        val result8 = hexToBigDecimal("0xE8D4A51000", BigDecimal.ZERO)
        assertEquals(result8, BigDecimal("1000000000000"))

        val result9 = hexToBigDecimal("270.0", BigDecimal.ZERO)
        assertEquals(result9, BigDecimal("0"))

        val result10 = hexToBigDecimal("270,0", BigDecimal.ZERO)
        assertEquals(result10, BigDecimal("0"))
    }

    @Test
    fun `is hex prefix fix`(){
        val result1 = containsHexPrefix("0x")
        assertEquals(result1, true)

        val result2 = containsHexPrefix("12easd")
        assertEquals(result2, false)

        val result3 = containsHexPrefix("270")
        assertEquals(result3, false)

        val result4 = containsHexPrefix("0xDE0B6B3A7640000")
        assertEquals(result4, true)
    }

    @Test
    fun `invalid hex string returns null`(){
        val result1 = hexToBigDecimal("E8D4A51000")
        assertNull(result1)

        val result2 = hexToBigDecimal("null")
        assertNull(result2)

        val result3 = hexToBigDecimal("")
        assertNull(result3)

        val result4 = hexToBigDecimal("0x")
        assertNull(result4)
    }

    @Test
    fun `hex string returns big decimal`(){
        val result1 = hexToBigDecimal("0xea300b17a8b780000 ")
        assertEquals(result1, BigDecimal("270000000000000000000"))

        val result2 = hexToBigDecimal("0x0")
        assertEquals(result2, BigDecimal("0"))

        val result3 = hexToBigDecimal("270")
        assertEquals(result3, BigDecimal("270"))

        val result4 = hexToBigDecimal("0x6f05b59d3b20000")
        assertEquals(result4, BigDecimal("500000000000000000"))

        val result5 = hexToBigDecimal("0x16345785D8A0000")
        assertEquals(result5, BigDecimal("100000000000000000"))

        val result6 = hexToBigDecimal("0xDE0B6B3A7640000")
        assertEquals(result6, BigDecimal("1000000000000000000"))

        val result7 = hexToBigDecimal("0xE8D4A51000")
        assertEquals(result7, BigDecimal("1000000000000"))

    }
}